package com.pttlan.domain.ptt.model

data class VoiceMessage(
    val id: String,
    val channelId: String,
    val senderNickname: String,
    val filePath: String,
    val durationMs: Long,
    val recordedAt: Long,
    /** When it was last replayed to the end; null while it has not been heard in the history. */
    val playedAt: Long? = null,
)
