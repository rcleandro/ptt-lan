package com.pttlan.domain.ptt.repository

import kotlinx.coroutines.flow.Flow

data class ChannelDomain(
    val id: String,
    val name: String,
    val isFavorite: Boolean
)

interface ChannelRepository {
    fun getRecentChannels(): Flow<List<ChannelDomain>>
    suspend fun saveChannel(channel: ChannelDomain)
}
