package com.pttlan.domain.ptt.usecase

import com.pttlan.domain.ptt.model.ParticipantDomain
import com.pttlan.domain.ptt.repository.ChannelSessionRepository
import com.pttlan.domain.ptt.repository.SpeakerState
import kotlinx.coroutines.flow.Flow

/**
 * UseCase responsável por enviar a solicitação de entrada em um canal PTT.
 */
class JoinChannelUseCase(
    private val repo: ChannelSessionRepository,
) {
    suspend operator fun invoke(
        channelId: String,
        userId: String,
        nickname: String,
    ) = repo.joinChannel(channelId, userId, nickname)
}

/**
 * UseCase responsável por solicitar a saída (leave) de um canal PTT.
 */
class LeaveChannelUseCase(
    private val repo: ChannelSessionRepository,
) {
    suspend operator fun invoke(
        channelId: String,
        userId: String,
    ) = repo.leaveChannel(channelId, userId)
}

/**
 * UseCase responsável por observar a lista de participantes ativos em um canal.
 */
class ObserveParticipantsUseCase(
    private val repo: ChannelSessionRepository,
) {
    operator fun invoke(channelId: String): Flow<List<ParticipantDomain>> = repo.observeParticipants(channelId)
}

/**
 * UseCase responsável por observar quem está com a posse da palavra (floor) em um canal.
 */
class ObserveSpeakerUseCase(
    private val repo: ChannelSessionRepository,
) {
    operator fun invoke(channelId: String): Flow<SpeakerState> = repo.observeSpeaker(channelId)
}

/**
 * UseCase responsável por observar eventos de negação de fala (ex: quando alguém já está falando).
 */
class ObserveFloorDeniedUseCase(
    private val repo: ChannelSessionRepository,
) {
    operator fun invoke(channelId: String): Flow<String> = repo.observeFloorDenied(channelId)
}
