package com.pttlan.domain.ptt.repository

import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Recorded messages: listing, replay and deletion. Split from [VoiceRepository] in 22.7, which keeps only
 * the live path (floor control, transmission and reception).
 */
interface HistoryRepository {
    /** Progress of the message being replayed, or null when nothing is playing. */
    val playbackPosition: StateFlow<PlaybackPosition?>

    /** Replay speed, 1 by default; it keeps the voice's pitch and is kept for the next time the app opens. */
    val playbackSpeed: StateFlow<Float>

    fun getAllMessages(): Flow<List<VoiceMessage>>

    suspend fun playMessage(message: VoiceMessage)

    suspend fun pausePlayingMessage()

    suspend fun resumePlayingMessage()

    suspend fun stopPlayingMessage()

    /** Moves the replay to [positionMs], clamped to the message; does nothing when nothing is playing. */
    suspend fun seekTo(positionMs: Long)

    /** Applies to the message playing and to the ones after it. */
    suspend fun setPlaybackSpeed(speed: Float)

    /** Writes [message] as a `.wav` in [directory] and returns its path, for sharing; null if its audio is gone. */
    suspend fun exportAsWav(
        message: VoiceMessage,
        directory: String,
    ): String?

    suspend fun clearAllMessages()

    suspend fun deleteMessage(message: VoiceMessage)

    suspend fun deleteChannelMessages(channelId: String)
}
