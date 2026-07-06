package com.pttlan.domain.ptt.model

data class VoiceMessage(
    val id: String,
    val channelId: String,
    val senderNickname: String,
    val filePath: String,
    val durationMs: Long,
    val recordedAt: Long,
)
