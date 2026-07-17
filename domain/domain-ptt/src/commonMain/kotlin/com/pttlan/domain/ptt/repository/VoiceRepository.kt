package com.pttlan.domain.ptt.repository
import com.pttlan.domain.ptt.model.VoiceMessage
import kotlinx.coroutines.flow.Flow

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
    fun getRecentMessages(channelId: String): Flow<List<VoiceMessage>>

    fun getAllMessages(): Flow<List<VoiceMessage>>

    suspend fun playMessage(message: VoiceMessage)

    suspend fun stopPlayingMessage()
}
