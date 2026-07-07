package com.pttlan.domain.ptt.usecase

import com.pttlan.domain.ptt.model.ParticipantDomain
import com.pttlan.domain.ptt.repository.ChannelSessionRepository
import com.pttlan.domain.ptt.repository.SpeakerState
import kotlinx.coroutines.flow.Flow

class JoinChannelUseCase(
    private val repo: ChannelSessionRepository,
) {
    suspend operator fun invoke(
        channelId: String,
        userId: String,
        nickname: String,
    ) = repo.joinChannel(channelId, userId, nickname)
}

class LeaveChannelUseCase(
    private val repo: ChannelSessionRepository,
) {
    suspend operator fun invoke(
        channelId: String,
        userId: String,
    ) = repo.leaveChannel(channelId, userId)
}

class ObserveParticipantsUseCase(
    private val repo: ChannelSessionRepository,
) {
    operator fun invoke(channelId: String): Flow<List<ParticipantDomain>> = repo.observeParticipants(channelId)
}

class ObserveSpeakerUseCase(
    private val repo: ChannelSessionRepository,
) {
    operator fun invoke(channelId: String): Flow<SpeakerState> = repo.observeSpeaker(channelId)
}

class ObserveFloorDeniedUseCase(
    private val repo: ChannelSessionRepository,
) {
    operator fun invoke(channelId: String): Flow<String> = repo.observeFloorDenied(channelId)
}
