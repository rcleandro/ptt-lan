package com.pttlan.domain.ptt.repository

import kotlinx.coroutines.flow.Flow

data class ChannelDomain(
    val id: String,
    val name: String,
    val isFavorite: Boolean,
)

data class ActiveChannelDomain(
    val id: String,
    val participantCount: Int,
)

interface ChannelRepository {
    fun getRecentChannels(): Flow<List<ChannelDomain>>

    fun observeActiveChannels(): Flow<List<ActiveChannelDomain>>

    suspend fun saveChannel(channel: ChannelDomain)
}
