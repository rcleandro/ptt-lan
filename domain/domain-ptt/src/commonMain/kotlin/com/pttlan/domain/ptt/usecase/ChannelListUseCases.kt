package com.pttlan.domain.ptt.usecase

import com.pttlan.domain.ptt.repository.ActiveChannelDomain
import com.pttlan.domain.ptt.repository.ChannelDomain
import com.pttlan.domain.ptt.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow

class GetRecentChannelsUseCase(
    private val channelRepository: ChannelRepository,
) {
    operator fun invoke(): Flow<List<ChannelDomain>> = channelRepository.getRecentChannels()
}

class ObserveActiveChannelsUseCase(
    private val channelRepository: ChannelRepository,
) {
    operator fun invoke(): Flow<List<ActiveChannelDomain>> = channelRepository.observeActiveChannels()
}

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
