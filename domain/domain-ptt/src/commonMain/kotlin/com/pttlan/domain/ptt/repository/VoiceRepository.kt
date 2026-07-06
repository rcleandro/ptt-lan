package com.pttlan.domain.ptt.repository

interface VoiceRepository {
    suspend fun requestFloor(channelId: String, userId: String)
    suspend fun releaseFloor(channelId: String, userId: String)
    suspend fun startTransmitting(channelId: String, userId: String)
    suspend fun stopTransmitting()
}
