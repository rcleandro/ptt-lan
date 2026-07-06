package com.pttlan.core.audio

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class IosAudioRecorder : AudioRecorder {
    override fun startCapture(sampleRate: Int): Flow<ByteArray> = flow {
        // TODO: Implement AVAudioEngine capture for iOS in a future phase
        println("IosAudioRecorder: Not implemented for MVP")
    }

    override fun stopCapture() {
    }
}

class IosAudioPlayer : AudioPlayer {
    override fun play(chunk: ByteArray, sampleRate: Int) {
        // TODO: Implement AVAudioEngine playback for iOS in a future phase
    }

    override fun stop() {
    }
}

class IosMicrophonePermissionManager : MicrophonePermissionManager {
    override suspend fun isGranted(): Boolean {
        return true
    }

    override suspend fun request(): Boolean {
        return true
    }
}

actual fun createAudioRecorder(): AudioRecorder = IosAudioRecorder()
actual fun createAudioPlayer(): AudioPlayer = IosAudioPlayer()
