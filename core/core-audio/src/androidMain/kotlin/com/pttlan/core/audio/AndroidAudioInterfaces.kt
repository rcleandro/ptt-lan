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

    override fun play(
        chunk: ByteArray,
        sampleRate: Int,
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
        }

        audioTrack?.write(chunk, 0, chunk.size)
    }

    override fun stop() {
        try {
            if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack?.stop()
            }
        } catch (e: IllegalStateException) {
            // Ignored - AudioTrack might already be stopped or uninitialized
        }
        audioTrack?.release()
        audioTrack = null
    }
}

actual fun createAudioRecorder(): AudioRecorder = AndroidAudioRecorder()

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
