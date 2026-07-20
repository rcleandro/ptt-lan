package com.pttlan.core.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.TimeUnit

private data class AudioPacket(
    val chunk: ByteArray,
    val sequenceNumber: Int,
    val timestampMs: Long,
) : Comparable<AudioPacket> {
    override fun compareTo(other: AudioPacket): Int = this.sequenceNumber.compareTo(other.sequenceNumber)
}

class AndroidAudioRecorder : AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    @SuppressLint("MissingPermission")
    override fun startCapture(sampleRate: Int): Flow<ByteArray> =
        flow {
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            val chunkSizeBytes = sampleRate * 2 * 20 / 1000
            val finalBufferSize = maxOf(bufferSize, chunkSizeBytes * 4)

            audioRecord =
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    finalBufferSize,
                )

            audioRecord?.startRecording()
            isRecording = true

            val buffer = ByteArray(chunkSizeBytes)

            try {
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        emit(buffer.copyOf(read))
                    }
                }
            } finally {
                try {
                    if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                        audioRecord?.stop()
                    }
                } catch (_: IllegalStateException) {
                    // Ignored - AudioRecord might already be stopped or uninitialized
                }
                audioRecord?.release()
                audioRecord = null
            }
        }.flowOn(Dispatchers.IO)

    override fun stopCapture() {
        isRecording = false
    }
}

class AndroidAudioPlayer : AudioPlayer {
    private var audioTrack: AudioTrack? = null
    private val queue = PriorityBlockingQueue<AudioPacket>()
    private var isPlaying = false
    private var playThread: Thread? = null
    private var expectedSequenceNumber = -1

    override fun play(
        chunk: ByteArray,
        sampleRate: Int,
        sequenceNumber: Int,
        timestampMs: Long,
    ) {
        if (audioTrack == null) {
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            audioTrack =
                AudioTrack
                    .Builder()
                    .setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    ).setAudioFormat(
                        AudioFormat
                            .Builder()
                            .setEncoding(audioFormat)
                            .setSampleRate(sampleRate)
                            .setChannelMask(channelConfig)
                            .build(),
                    ).setBufferSizeInBytes(maxOf(bufferSize, chunk.size * 4))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            audioTrack?.play()

            isPlaying = true
            playThread =
                Thread {
                    var isBuffering = true
                    while (isPlaying) {
                        if (isBuffering) {
                            if (queue.size < 5) {
                                Thread.sleep(10)
                                continue
                            } else {
                                isBuffering = false
                            }
                        }

                        val packet = queue.poll(500, TimeUnit.MILLISECONDS)
                        if (packet != null && audioTrack != null) {
                            if (expectedSequenceNumber == -1 || packet.sequenceNumber < expectedSequenceNumber - 20) {
                                expectedSequenceNumber = packet.sequenceNumber
                            }
                            if (packet.sequenceNumber >= expectedSequenceNumber) {
                                audioTrack?.write(packet.chunk, 0, packet.chunk.size)
                                expectedSequenceNumber = packet.sequenceNumber + 1
                            }
                        } else {
                            isBuffering = true
                            expectedSequenceNumber = -1
                        }
                    }
                }
            playThread?.isDaemon = true
            playThread?.start()
        }

        queue.offer(AudioPacket(chunk, sequenceNumber, timestampMs))
    }

    override fun stop() {
        isPlaying = false
        try {
            playThread?.join(500)
        } catch (_: Exception) {
            // Ignore
        }
        playThread = null

        try {
            if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack?.stop()
            }
        } catch (_: IllegalStateException) {
            // Ignored - AudioTrack might already be stopped or uninitialized
        }
        audioTrack?.release()
        audioTrack = null
        queue.clear()
        expectedSequenceNumber = -1
    }
}

actual fun createAudioRecorder(): AudioRecorder = AndroidAudioRecorder()

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
