package com.pttlan.data.ptt.repository

import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.domain.ptt.model.ParticipantDomain
import com.pttlan.domain.ptt.repository.ChannelSessionRepository
import com.pttlan.domain.ptt.repository.SpeakerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

class ChannelSessionRepositoryImpl(
    private val webSocketClient: PttWebSocketClient,
) : ChannelSessionRepository {
    override suspend fun joinChannel(
        channelId: String,
        userId: String,
        nickname: String,
    ) {
        webSocketClient.sendControlMessage(
            ControlMessage.JoinChannel(
                channelId = channelId,
                nickname = nickname,
                userId = userId,
            ),
        )
    }

    override suspend fun leaveChannel(
        channelId: String,
        userId: String,
    ) {
        webSocketClient.sendControlMessage(
            ControlMessage.LeaveChannel(
                channelId = channelId,
                userId = userId,
            ),
        )
    }

    override fun observeParticipants(channelId: String): Flow<List<ParticipantDomain>> =
        webSocketClient.controlMessages
            .filterIsInstance<ControlMessage.ParticipantList>()
            .filter { it.channelId == channelId }
            .map { msg ->
                msg.participants.map {
                    ParticipantDomain(it.userId, it.nickname, it.isSpeaking)
                }
            }

    override fun observeSpeaker(channelId: String): Flow<SpeakerState> =
        webSocketClient.controlMessages
            .filterIsInstance<ControlMessage.SpeakerChanged>()
            .filter { it.channelId == channelId }
            .map { msg ->
                SpeakerState(msg.userId, msg.nickname, msg.isSpeaking)
            }

    override fun observeFloorDenied(channelId: String): Flow<String> =
        webSocketClient.controlMessages
            .filterIsInstance<ControlMessage.FloorDenied>()
            .filter { it.channelId == channelId }
            .map { it.reason }
}
