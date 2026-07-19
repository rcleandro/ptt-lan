package com.pttlan.core.audio

import kotlinx.coroutines.flow.Flow

interface AudioRecorder {
    fun startCapture(sampleRate: Int = 48_000): Flow<ByteArray>

    fun stopCapture()
}

interface AudioPlayer {
    fun play(
        chunk: ByteArray,
        sampleRate: Int = 48_000,
    )

    fun stop()
}

interface AudioCodec {
    fun encode(pcm: ByteArray): ByteArray

    fun decode(encoded: ByteArray): ByteArray
}

expect fun createAudioRecorder(): AudioRecorder

expect fun createAudioPlayer(): AudioPlayer
