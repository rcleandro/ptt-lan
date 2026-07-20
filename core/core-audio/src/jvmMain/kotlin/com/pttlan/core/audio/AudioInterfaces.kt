package com.pttlan.core.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
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
                var offset = 0
                while (isRecording) {
                    val bytesRead = line?.read(buffer, offset, buffer.size - offset) ?: 0
                    if (bytesRead > 0) {
                        offset += bytesRead
                        if (offset == buffer.size) {
                            emit(buffer.copyOf())
                            offset = 0
                        }
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
    private val queue = LinkedBlockingQueue<ByteArray>()
    private var isPlaying = false
    private var playThread: Thread? = null

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

                        val c = queue.poll(500, TimeUnit.MILLISECONDS)
                        if (c != null && line != null) {
                            line?.write(c, 0, c.size)
                        } else {
                            isBuffering = true
                        }
                    }
                }
            playThread?.isDaemon = true
            playThread?.start()
        }

        queue.offer(chunk)
    }

    override fun stop() {
        isPlaying = false
        try {
            playThread?.join(500)
        } catch (_: Exception) {
            // Ignore
        }
        playThread = null

        line?.drain()
        line?.stop()
        line?.close()
        line = null
        queue.clear()
    }
}

actual fun createAudioRecorder(): AudioRecorder = JvmAudioRecorder()

actual fun createAudioPlayer(): AudioPlayer = JvmAudioPlayer()
