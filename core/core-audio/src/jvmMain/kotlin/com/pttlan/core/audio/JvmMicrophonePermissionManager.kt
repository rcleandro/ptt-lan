package com.pttlan.core.audio

class JvmMicrophonePermissionManager : MicrophonePermissionManager {
    override suspend fun isGranted(): Boolean {
        // Desktop permissions are generally OS-level and we assume they are granted
        return true
    }

    override suspend fun request(): Boolean = true
}
