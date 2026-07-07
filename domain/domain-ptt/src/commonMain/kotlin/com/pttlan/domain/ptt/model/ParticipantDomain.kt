package com.pttlan.domain.ptt.model

data class ParticipantDomain(
    val userId: String,
    val nickname: String,
    val isSpeaking: Boolean,
)
