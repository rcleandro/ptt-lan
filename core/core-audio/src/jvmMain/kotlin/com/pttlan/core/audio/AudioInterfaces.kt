package com.pttlan.core.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine

class JvmAudioRecorder : AudioRecorder {
    private var line: TargetDataLine? = null
    private var isRecording = false

    override fun startCapture(sampleRate: Int): Flow<ByteArray> =
        flow {
            val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)

            if (!AudioSystem.isLineSupported(info)) {
                throw Exception("Line not supported")
            }

            line = AudioSystem.getLine(info) as TargetDataLine
            line?.open(format)
            line?.start()

            isRecording = true
            val chunkSizeBytes = sampleRate * 2 * 20 / 1000
            val buffer = ByteArray(chunkSizeBytes)

            try {
                while (isRecording) {
                    val bytesRead = line?.read(buffer, 0, buffer.size) ?: 0
                    if (bytesRead > 0) {
                        emit(buffer.copyOf(bytesRead))
                    }
                }
            } finally {
                line?.stop()
                line?.close()
            }
        }.flowOn(Dispatchers.IO)

    override fun stopCapture() {
        isRecording = false
    }
}

class JvmAudioPlayer : AudioPlayer {
    private var line: SourceDataLine? = null

    override fun play(
        chunk: ByteArray,
        sampleRate: Int,
    ) {
        if (line == null) {
            val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            val info = DataLine.Info(SourceDataLine::class.java, format)
            line = AudioSystem.getLine(info) as SourceDataLine
            line?.open(format)
            line?.start()
        }

        line?.write(chunk, 0, chunk.size)
    }

    override fun stop() {
        line?.drain()
        line?.stop()
        line?.close()
        line = null
    }
}

actual fun createAudioRecorder(): AudioRecorder = JvmAudioRecorder()

actual fun createAudioPlayer(): AudioPlayer = JvmAudioPlayer()
