package com.pttlan.domain.ptt.repository

interface VoiceRepository {
    suspend fun startTransmitting(channelId: String, userId: String)
    suspend fun stopTransmitting()
}
