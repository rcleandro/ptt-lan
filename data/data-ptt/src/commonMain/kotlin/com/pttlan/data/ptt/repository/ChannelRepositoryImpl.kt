package com.pttlan.data.ptt.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.pttlan.core.database.PttDatabase
import com.pttlan.domain.ptt.repository.ChannelDomain
import com.pttlan.domain.ptt.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class ChannelRepositoryImpl(
    private val database: PttDatabase,
) : ChannelRepository {
    private val queries = database.channelQueries

    override fun getRecentChannels(): Flow<List<ChannelDomain>> =
        queries
            .selectAllChannels()
            .asFlow()
            .mapToList(Dispatchers.Default) // Needs kotlinx-coroutines-core for Default, we'll provide if needed
            .map { list ->
                list.map {
                    ChannelDomain(
                        id = it.id,
                        name = it.name,
                        isFavorite = it.isFavorite,
                    )
                }
            }

    override suspend fun saveChannel(channel: ChannelDomain) {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertOrReplaceChannel(
            id = channel.id,
            name = channel.name,
            lastJoinedAt = now,
            isFavorite = channel.isFavorite,
        )
    }
}
