package com.pttlan.data.ptt.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.domain.ptt.repository.ActiveChannelDomain
import com.pttlan.domain.ptt.repository.ChannelDomain
import com.pttlan.domain.ptt.repository.ChannelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock

class ChannelRepositoryImpl(
    database: PttDatabase,
    private val webSocketClient: PttWebSocketClient,
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

    private val activeChannelsFlow = MutableStateFlow<List<ActiveChannelDomain>>(emptyList())

    init {
        CoroutineScope(Dispatchers.Default).launch {
            webSocketClient.controlMessages
                .filterIsInstance<ControlMessage.ActiveChannelsList>()
                .collect { message ->
                    activeChannelsFlow.value =
                        message.activeChannels.map {
                            ActiveChannelDomain(
                                id = it.channelId,
                                participantCount = it.participantCount,
                            )
                        }
                }
        }
    }

    override fun observeActiveChannels(): Flow<List<ActiveChannelDomain>> = activeChannelsFlow

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
