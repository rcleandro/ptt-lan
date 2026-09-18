package com.pttlan.domain.ptt.repository

import com.pttlan.domain.ptt.model.VoiceMessage
import kotlinx.coroutines.flow.Flow

/**
 * Recorded messages: listing, replay and deletion. Split from [VoiceRepository] in 22.7, which keeps only
 * the live path (floor control, transmission and reception).
 */
interface HistoryRepository {
    fun getRecentMessages(channelId: String): Flow<List<VoiceMessage>>

    fun getAllMessages(): Flow<List<VoiceMessage>>

    suspend fun playMessage(message: VoiceMessage)

    suspend fun pausePlayingMessage()

    suspend fun resumePlayingMessage()

    suspend fun stopPlayingMessage()

    suspend fun clearAllMessages()

    suspend fun deleteMessage(message: VoiceMessage)

    suspend fun deleteChannelMessages(channelId: String)
}
