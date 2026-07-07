@file:Suppress("LongMethod", "CyclomaticComplexMethod", "TooGenericExceptionCaught", "MaxLineLength")

package com.pttlan.server.routing

import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.server.channel.ChannelRegistry
import com.pttlan.server.channel.Participant
import io.ktor.server.routing.Routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Routing.pttRoutes() {
    val channelRegistry by inject<ChannelRegistry>()

    webSocket("/ws") {
        var currentUserId: String? = null
        var currentChannelId: String? = null

        try {
            val nickname = call.request.queryParameters["nickname"] ?: "Desconhecido"
            if (!channelRegistry.addGlobalConnection(this, nickname)) {
                close(io.ktor.websocket.CloseReason(io.ktor.websocket.CloseReason.Codes.VIOLATED_POLICY, "Nome já em uso"))
                return@webSocket
            }
            println("Novo client conectado via WebSocket! ($nickname)")
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        try {
                            when (val message = Json.decodeFromString<ControlMessage>(text)) {
                                is ControlMessage.JoinChannel -> {
                                    currentUserId = message.userId
                                    currentChannelId = message.channelId
                                    println("Usuário ${message.nickname} (${message.userId}) entrou no canal ${message.channelId}")

                                    val channel = channelRegistry.getOrCreateChannel(message.channelId)
                                    val participant = Participant(message.userId, message.nickname, this)
                                    channel.addParticipant(participant)
                                    channelRegistry.broadcastActiveChannels()
                                }
                                is ControlMessage.LeaveChannel -> {
                                    val channel = channelRegistry.getChannel(message.channelId)
                                    val participantNickname = channel?.getParticipant(message.userId)?.nickname ?: "Desconhecido"
                                    println("Usuário $participantNickname (${message.userId}) saiu do canal ${message.channelId}")
                                    channel?.removeParticipant(message.userId)
                                    channelRegistry.scheduleCleanupIfEmpty(message.channelId)
                                    channelRegistry.broadcastActiveChannels()
                                }
                                is ControlMessage.StartSpeaking -> {
                                    val channel = channelRegistry.getChannel(message.channelId)
                                    if (channel != null) {
                                        val granted = channel.requestFloor(message.userId)
                                        if (!granted) {
                                            val json =
                                                Json.encodeToString<ControlMessage>(
                                                    ControlMessage.FloorDenied(message.channelId, "Alguém já está falando"),
                                                )
                                            send(Frame.Text(json))
                                        }
                                    }
                                }
                                is ControlMessage.StopSpeaking -> {
                                    val channel = channelRegistry.getChannel(message.channelId)
                                    channel?.releaseFloor(message.userId)
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
                            val channel = channelRegistry.getChannel(currentChannelId)
                            channel?.broadcastBinary(frame, currentUserId)
                        }
                    }
                    else -> {}
                }
            }
        } finally {
            val channel = currentChannelId?.let { channelRegistry.getChannel(it) }
            val nickname = currentUserId?.let { channel?.getParticipant(it)?.nickname } ?: "Desconhecido"
            println("Client desconectado via WebSocket! (User: $nickname [$currentUserId])")
            if (currentUserId != null && currentChannelId != null) {
                channel?.removeParticipant(currentUserId)
                channelRegistry.scheduleCleanupIfEmpty(currentChannelId)
            }
            channelRegistry.removeGlobalConnection(this)
        }
    }
}
