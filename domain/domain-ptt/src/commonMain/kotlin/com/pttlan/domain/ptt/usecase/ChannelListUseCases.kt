package com.pttlan.domain.ptt.usecase

import com.pttlan.domain.ptt.repository.ActiveChannelDomain
import com.pttlan.domain.ptt.repository.ChannelDomain
import com.pttlan.domain.ptt.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase responsável por obter a lista de canais recentes (acessados anteriormente).
 */
class GetRecentChannelsUseCase(
    private val channelRepository: ChannelRepository,
) {
    operator fun invoke(): Flow<List<ChannelDomain>> = channelRepository.getRecentChannels()
}

/**
 * UseCase responsável por observar os canais ativos no servidor atual.
 */
class ObserveActiveChannelsUseCase(
    private val channelRepository: ChannelRepository,
) {
    operator fun invoke(): Flow<List<ActiveChannelDomain>> = channelRepository.observeActiveChannels()
}

/**
 * UseCase responsável por registrar o ingresso em um canal para salvar no histórico recente.
 */
class JoinChannelUseCaseImpl(
    private val channelRepository: ChannelRepository,
) {
    suspend operator fun invoke(
        channelId: String,
        name: String,
    ) {
        channelRepository.saveChannel(ChannelDomain(channelId, name, false))
    }
}

/**
 * UseCase responsável por criar e salvar um novo canal.
 */
class CreateChannelUseCase(
    private val channelRepository: ChannelRepository,
) {
    suspend operator fun invoke(name: String): String {
        if (name.isBlank()) throw IllegalArgumentException("Channel name cannot be empty")
        val id = name.lowercase().replace(" ", "-")
        channelRepository.saveChannel(ChannelDomain(id, name, false))
        return id
    }
}
