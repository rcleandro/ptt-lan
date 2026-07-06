package com.pttlan.server.routing

import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.server.channel.ChannelRegistry
import com.pttlan.server.channel.Participant
import io.ktor.server.routing.Routing
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Routing.pttRoutes() {
    val channelRegistry by inject<ChannelRegistry>()

    webSocket("/ws") {
        var currentUserId: String? = null
        var currentChannelId: String? = null

        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        try {
                            val message = Json.decodeFromString<ControlMessage>(text)
                            when (message) {
                                is ControlMessage.JoinChannel -> {
                                    currentUserId = message.userId
                                    currentChannelId = message.channelId
                                    
                                    val channel = channelRegistry.getOrCreateChannel(message.channelId)
                                    val participant = Participant(message.userId, message.nickname, this)
                                    channel.addParticipant(participant)
                                }
                                is ControlMessage.LeaveChannel -> {
                                    val channel = channelRegistry.getChannel(message.channelId)
                                    channel?.removeParticipant(message.userId)
                                }
                                is ControlMessage.StartSpeaking -> {
                                    val channel = channelRegistry.getChannel(message.channelId)
                                    channel?.updateSpeakingStatus(message.userId, true)
                                }
                                is ControlMessage.StopSpeaking -> {
                                    val channel = channelRegistry.getChannel(message.channelId)
                                    channel?.updateSpeakingStatus(message.userId, false)
                                }
                                is ControlMessage.Heartbeat -> {
                                    // Could track last heartbeat for automatic cleanup
                                }
                                else -> {} // ParticipantList, SpeakerChanged, FloorDenied are Server -> Client
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    is Frame.Binary -> {
                        // Audio chunks broadcast (MVP: blindly broadcast to everyone else in the channel)
                        if (currentChannelId != null && currentUserId != null) {
                            val channel = channelRegistry.getChannel(currentChannelId!!)
                            channel?.broadcastBinary(frame, currentUserId!!)
                        }
                    }
                    else -> {}
                }
            }
        } finally {
            if (currentUserId != null && currentChannelId != null) {
                val channel = channelRegistry.getChannel(currentChannelId!!)
                channel?.removeParticipant(currentUserId!!)
            }
        }
    }
}
