@file:OptIn(ExperimentalForeignApi::class)

package com.pttlan.core.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.set
import kotlinx.cinterop.value
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFAudio.AVAudioConverter
import platform.AVFAudio.AVAudioConverterInputStatus_HaveData
import platform.AVFAudio.AVAudioConverterInputStatus_NoDataNow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatInt16
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFAudio.setPreferredSampleRate
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSError

/** 20 ms at 48 kHz: the frame size Opus accepts and the same one Android and the JVM emit. */
private const val FRAME_SAMPLES = 960
private const val FRAME_BYTES = FRAME_SAMPLES * 2

class IosAudioRecorder : AudioRecorder {
    private var audioEngine: AVAudioEngine? = null
    private var isRecording = false

    /** Converted audio waiting to complete a frame; the hardware tap size never matches ours. */
    private var pending = ByteArray(0)

    override fun startCapture(sampleRate: Int): Flow<ByteArray> =
        callbackFlow {
            try {
                val session = AVAudioSession.sharedInstance()
                session.setCategory(AVAudioSessionCategoryPlayAndRecord, AVAudioSessionCategoryOptionDefaultToSpeaker, null)
                session.setPreferredSampleRate(sampleRate.toDouble(), null)
                session.setActive(true, null)

                val engine = AVAudioEngine()
                audioEngine = engine

                val inputNode = engine.inputNode
                val inputFormat = inputNode.inputFormatForBus(0.toULong())

                // The microphone runs at whatever rate the hardware picked (often 44.1 kHz) in Float32.
                // AVAudioConverter brings it to 48 kHz Int16 mono; the old code decimated by an integer step,
                // which both detuned the audio and produced frame sizes Opus rejects.
                val targetFormat =
                    AVAudioFormat(
                        AVAudioPCMFormatInt16,
                        sampleRate.toDouble(),
                        1.toUInt(),
                        false,
                    )
                val converter = AVAudioConverter(fromFormat = inputFormat, toFormat = targetFormat)
                if (converter == null) {
                    close(IllegalStateException("Não foi possível converter ${inputFormat.sampleRate}Hz para $sampleRate Hz"))
                    return@callbackFlow
                }

                pending = ByteArray(0)
                inputNode.installTapOnBus(
                    bus = 0.toULong(),
                    bufferSize = 2048.toUInt(),
                    format = inputFormat,
                ) { buffer, _ ->
                    if (buffer == null) return@installTapOnBus
                    val converted = convertToPcm16(buffer, converter, targetFormat) ?: return@installTapOnBus

                    pending += converted
                    while (pending.size >= FRAME_BYTES) {
                        trySend(pending.copyOfRange(0, FRAME_BYTES))
                        pending = pending.copyOfRange(FRAME_BYTES, pending.size)
                    }
                }

                engine.prepare()
                engine.startAndReturnError(null)
                isRecording = true
                println("IosAudioRecorder: Captura de áudio iniciada com sucesso em ${inputFormat.sampleRate}Hz.")
            } catch (e: Exception) {
                println("IosAudioRecorder: Erro ao iniciar gravação: ${e.message}")
                close(e)
            }

            awaitClose {
                stopCapture()
            }
        }

    override fun stopCapture() {
        if (!isRecording) return
        try {
            pending = ByteArray(0)
            audioEngine?.inputNode?.removeTapOnBus(0.toULong())
            audioEngine?.stop()
            audioEngine = null
            isRecording = false
            println("IosAudioRecorder: Gravação interrompida com sucesso.")
        } catch (e: Exception) {
            println("IosAudioRecorder: Erro ao parar gravação: ${e.message}")
        }
    }
}

/** Runs one tap buffer through the converter and returns it as little-endian PCM16 bytes. */
@Suppress("ReturnCount")
private fun convertToPcm16(
    input: AVAudioPCMBuffer,
    converter: AVAudioConverter,
    targetFormat: AVAudioFormat,
): ByteArray? {
    val ratio = targetFormat.sampleRate / input.format.sampleRate
    val capacity = (input.frameLength.toDouble() * ratio).toUInt() + FRAME_SAMPLES.toUInt()
    val output = AVAudioPCMBuffer(targetFormat, capacity)
    var delivered = false

    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        converter.convertToBuffer(output, error.ptr) { _, statusPointer ->
            if (delivered) {
                statusPointer?.pointed?.value = AVAudioConverterInputStatus_NoDataNow
                null
            } else {
                delivered = true
                statusPointer?.pointed?.value = AVAudioConverterInputStatus_HaveData
                input
            }
        }
    }

    val frames = output.frameLength.toInt()
    if (frames == 0) return null
    val channelData = output.int16ChannelData ?: return null
    val samples = channelData[0] ?: return null

    val bytes = ByteArray(frames * 2)
    for (i in 0 until frames) {
        val sample = samples[i].toInt()
        bytes[i * 2] = (sample and 0xFF).toByte()
        bytes[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
    }
    return bytes
}

class IosAudioPlayer : AudioPlayer {
    private var audioEngine: AVAudioEngine? = null
    private var playerNode: AVAudioPlayerNode? = null
    private var isPlaying = false

    private fun initEngine(sampleRate: Int): Pair<AVAudioEngine, AVAudioPlayerNode> {
        val engine = AVAudioEngine()
        val player = AVAudioPlayerNode()
        engine.attachNode(player)

        // Formato para reprodução interna no AVAudioEngine (PCM Float32 mono)
        val format =
            AVAudioFormat(
                standardFormatWithSampleRate = sampleRate.toDouble(),
                channels = 1.toUInt(),
            )

        engine.connect(player, to = engine.mainMixerNode, format = format)
        engine.prepare()
        engine.startAndReturnError(null)

        audioEngine = engine
        playerNode = player
        isPlaying = true
        return Pair(engine, player)
    }

    override fun play(
        chunk: ByteArray,
        sampleRate: Int,
        sequenceNumber: Int,
        timestampMs: Long,
    ) {
        try {
            val session = AVAudioSession.sharedInstance()
            if (session.category != AVAudioSessionCategoryPlayAndRecord &&
                session.category != AVAudioSessionCategoryPlayback
            ) {
                session.setCategory(AVAudioSessionCategoryPlayAndRecord, AVAudioSessionCategoryOptionDefaultToSpeaker, null)
                session.setActive(true, null)
            }

            val (engine, player) =
                if (audioEngine == null || playerNode == null) {
                    initEngine(sampleRate)
                } else {
                    Pair(audioEngine!!, playerNode!!)
                }

            // Conversão de bytes PCM16 para Float32
            val sampleCount = chunk.size / 2
            val floatArray = FloatArray(sampleCount)
            for (i in 0 until sampleCount) {
                val low = chunk[i * 2].toInt() and 0xFF
                val high = chunk[i * 2 + 1].toInt()
                val sample16 = ((high shl 8) or low).toShort()
                floatArray[i] = sample16.toFloat() / 32767.0f
            }

            val format =
                AVAudioFormat(
                    standardFormatWithSampleRate = sampleRate.toDouble(),
                    channels = 1.toUInt(),
                )
            val buffer = AVAudioPCMBuffer(format, sampleCount.toUInt())
            buffer.frameLength = sampleCount.toUInt()

            val floatChannelData = buffer.floatChannelData ?: return
            val floatData = floatChannelData[0] ?: return
            for (i in 0 until sampleCount) {
                floatData[i] = floatArray[i]
            }

            player.scheduleBuffer(buffer, completionHandler = null)
            if (!player.isPlaying()) {
                player.play()
            }
        } catch (e: Exception) {
            println("IosAudioPlayer: Erro ao reproduzir áudio: ${e.message}")
        }
    }

    override fun stop() {
        try {
            playerNode?.stop()
            audioEngine?.stop()
            playerNode = null
            audioEngine = null
            isPlaying = false
            println("IosAudioPlayer: Reprodução parada com sucesso.")
        } catch (e: Exception) {
            println("IosAudioPlayer: Erro ao parar reprodução: ${e.message}")
        }
    }
}

class IosMicrophonePermissionManager : MicrophonePermissionManager {
    override suspend fun isGranted(): Boolean {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio)
        return status == AVAuthorizationStatusAuthorized
    }

    override suspend fun request(): Boolean {
        val deferred = CompletableDeferred<Boolean>()
        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeAudio) { granted ->
            deferred.complete(granted)
        }
        return deferred.await()
    }
}

actual fun createAudioRecorder(): AudioRecorder = IosAudioRecorder()

actual fun createAudioPlayer(): AudioPlayer = IosAudioPlayer()
