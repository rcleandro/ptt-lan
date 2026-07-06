package com.pttlan.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioManager
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

            // Use a 32ms chunk size approximately
            val chunkSizeBytes = sampleRate * 2 * 32 / 1000
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
                audioRecord?.stop()
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
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    maxOf(bufferSize, chunk.size * 4),
                    AudioTrack.MODE_STREAM,
                )
            audioTrack?.play()
        }

        audioTrack?.write(chunk, 0, chunk.size)
    }

    override fun stop() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}

actual fun createAudioRecorder(): AudioRecorder = AndroidAudioRecorder()

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
