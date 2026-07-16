package com.pttlan.domain.ptt.repository

import com.pttlan.domain.ptt.model.ParticipantDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ChannelSessionRepository {
    val activeSessionChannelId: StateFlow<String?>

    suspend fun joinChannel(
        channelId: String,
        userId: String,
        nickname: String,
    )

    suspend fun leaveChannel(
        channelId: String,
        userId: String,
    )

    fun observeParticipants(channelId: String): Flow<List<ParticipantDomain>>

    fun observeSpeaker(channelId: String): Flow<SpeakerState>

    fun observeFloorDenied(channelId: String): Flow<String>
}

data class SpeakerState(
    val userId: String,
    val nickname: String,
    val isSpeaking: Boolean,
)
