package com.pttlan.core.audio

interface MicrophonePermissionManager {
    suspend fun isGranted(): Boolean
    suspend fun request(): Boolean
}
