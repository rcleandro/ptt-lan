package com.pttlan.domain.ptt.repository

interface VoiceRepository {
    suspend fun requestFloor(
        channelId: String,
        userId: String,
    )

    suspend fun releaseFloor(
        channelId: String,
        userId: String,
    )

    suspend fun startTransmitting(
        channelId: String,
        userId: String,
    )

    suspend fun stopTransmitting()

    // History
    fun getRecentMessages(channelId: String): kotlinx.coroutines.flow.Flow<List<com.pttlan.domain.ptt.model.VoiceMessage>>

    suspend fun playMessage(message: com.pttlan.domain.ptt.model.VoiceMessage)

    suspend fun stopPlayingMessage()
}
