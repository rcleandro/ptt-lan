@file:Suppress("ktlint:standard:no-wildcard-imports")
@file:OptIn(ExperimentalForeignApi::class)

package com.pttlan.core.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFAudio.*
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType

class IosAudioRecorder : AudioRecorder {
    private var audioEngine: AVAudioEngine? = null
    private var isRecording = false

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

                inputNode.installTapOnBus(
                    bus = 0.toULong(),
                    bufferSize = 2048.toUInt(),
                    format = inputFormat,
                ) { buffer, _ ->
                    if (buffer == null) return@installTapOnBus
                    val floatChannelData = buffer.floatChannelData ?: return@installTapOnBus
                    val floatData = floatChannelData[0] ?: return@installTapOnBus
                    val frameLength = buffer.frameLength.toInt()

                    val hwSampleRate = inputFormat.sampleRate
                    val ratio = maxOf(1.0, hwSampleRate / sampleRate.toDouble())
                    val step = ratio.toInt()

                    val targetLength = frameLength / step
                    val byteArray = ByteArray(targetLength * 2)

                    var outIndex = 0
                    for (i in 0 until frameLength step step) {
                        if (outIndex >= targetLength) break

                        val sample = floatData[i]
                        val intSample = (sample * 32767.0f).toInt().coerceIn(-32768, 32767)

                        byteArray[outIndex * 2] = (intSample and 0xFF).toByte()
                        byteArray[outIndex * 2 + 1] = ((intSample shr 8) and 0xFF).toByte()
                        outIndex++
                    }

                    trySend(byteArray)
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
