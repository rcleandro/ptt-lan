
package com.pttlan.server.routing

import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.server.auth.JwtConfig
import com.pttlan.server.channel.ChannelRegistry
import com.pttlan.server.channel.Participant
import io.ktor.server.plugins.origin
import io.ktor.server.routing.Routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

@Suppress("LongMethod", "CyclomaticComplexMethod", "TooGenericExceptionCaught", "MaxLineLength")
fun Routing.pttRoutes() {
    val channelRegistry by inject<ChannelRegistry>()

    webSocket("/ws") {
        var currentUserId: String? = null
        var currentChannelId: String? = null

        try {
            val token = call.request.queryParameters["token"]
            if (token.isNullOrBlank()) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token JWT ausente"))
                return@webSocket
            }

            val decodedJwt =
                try {
                    JwtConfig.verifier.verify(token)
                } catch (e: Exception) {
                    println("PttRoutes: Falha na validacao do token JWT: ${e.message}")
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token JWT inválido ou expirado"))
                    return@webSocket
                }

            // Identity comes only from the signed token; userId/nickname inside messages are ignored.
            val userId = decodedJwt.subject
            if (userId.isNullOrBlank()) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token JWT sem identidade"))
                return@webSocket
            }
            currentUserId = userId
            val nickname = decodedJwt.getClaim("nickname").asString() ?: "Desconhecido"
            val deviceId = decodedJwt.getClaim("deviceId").asString() ?: userId
            if (!channelRegistry.addGlobalConnection(this, nickname, deviceId)) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Nome já em uso"))
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
                                    currentChannelId = message.channelId
                                    println("Usuário $nickname ($userId) entrou no canal ${message.channelId}")

                                    val appVersion = call.request.queryParameters["version"] ?: "Desconhecida"
                                    val ipAddress = call.request.origin.remoteHost

                                    val channel = channelRegistry.getOrCreateChannel(message.channelId)
                                    val participant =
                                        Participant(
                                            userId = userId,
                                            nickname = nickname,
                                            session = this,
                                            isSpeaking = false,
                                            ipAddress = ipAddress,
                                            appVersion = appVersion,
                                            pingMs = 0L,
                                        )
                                    channel.addParticipant(participant)
                                    channelRegistry.broadcastActiveChannels()
                                }

                                is ControlMessage.LeaveChannel -> {
                                    val channel = channelRegistry.getChannel(message.channelId)
                                    println("Usuário $nickname ($userId) saiu do canal ${message.channelId}")
                                    channel?.removeParticipant(userId)
                                    channelRegistry.scheduleCleanupIfEmpty(message.channelId)
                                    channelRegistry.broadcastActiveChannels()
                                }

                                is ControlMessage.StartSpeaking -> {
                                    println("PttRoutes: Usuário $userId solicitou falar no canal ${message.channelId}")
                                    val channel = channelRegistry.getChannel(message.channelId)
                                    if (channel != null) {
                                        val granted = channel.requestFloor(userId)
                                        println("PttRoutes: Concessão da palavra para $userId: $granted")
                                        if (!granted) {
                                            val json =
                                                Json.encodeToString<ControlMessage>(
                                                    ControlMessage.FloorDenied(message.channelId, "Alguém já está falando"),
                                                )
                                            send(Frame.Text(json))
                                        }
                                    } else {
                                        println("PttRoutes: AVISO - Canal ${message.channelId} não encontrado ao solicitar fala")
                                    }
                                }

                                is ControlMessage.StopSpeaking -> {
                                    println("PttRoutes: Usuário $userId liberou a fala no canal ${message.channelId}")
                                    val channel = channelRegistry.getChannel(message.channelId)
                                    channel?.releaseFloor(userId)
                                }

                                is ControlMessage.Heartbeat -> {
                                    // Could track last heartbeat for automatic cleanup
                                }

                                else -> {} // ParticipantList, SpeakerChanged, FloorDenied are Server -> Client
                            }
                        } catch (e: Exception) {
                            println("Error in connection: ${e.message}")
                        }
                    }

                    is Frame.Binary -> {
                        if (currentChannelId != null && currentUserId != null) {
                            // Handled in order on this session's own coroutine: the broadcast only queues the
                            // packet per listener, so there is nothing slow left to hand to another coroutine.
                            val channel = channelRegistry.getChannel(currentChannelId)
                            try {
                                channel?.broadcastBinary(frame, currentUserId)
                            } catch (e: Exception) {
                                println("PttRoutes: Erro ao despachar broadcast de áudio: ${e.message}")
                            }
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
