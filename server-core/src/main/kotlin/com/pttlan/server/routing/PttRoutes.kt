
package com.pttlan.server.routing

import com.pttlan.core.network.PROTOCOL_VERSION
import com.pttlan.core.network.PttJson
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
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.pttlan.server.ptt")

@Suppress("LongMethod", "CyclomaticComplexMethod", "TooGenericExceptionCaught", "MaxLineLength")
fun Routing.pttRoutes() {
    val channelRegistry by inject<ChannelRegistry>()

    webSocket("/ws") {
        var currentUserId: String? = null
        var currentChannelId: String? = null

        try {
            val protocol = call.request.queryParameters["protocol"]?.toIntOrNull()
            // A client from before 21.5 sends no protocol at all; it speaks version 1, so it is let through.
            if (protocol != null && protocol != PROTOCOL_VERSION) {
                logger.info("Refusing client with protocol {} (server speaks {})", protocol, PROTOCOL_VERSION)
                close(
                    CloseReason(
                        CloseReason.Codes.VIOLATED_POLICY,
                        "Versão do app incompatível com o servidor. Atualize o aplicativo.",
                    ),
                )
                return@webSocket
            }

            val token = call.request.queryParameters["token"]
            if (token.isNullOrBlank()) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token JWT ausente"))
                return@webSocket
            }

            val decodedJwt =
                try {
                    JwtConfig.verifier.verify(token)
                } catch (e: Exception) {
                    logger.info("JWT validation failed: {}", e.message)
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
            logger.info("Client connected: {}", nickname)
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        try {
                            when (val message = PttJson.decodeFromString<ControlMessage>(text)) {
                                is ControlMessage.JoinChannel -> {
                                    val refusal = channelRegistry.refusalToOpen(message.channelId)
                                    if (refusal != null) {
                                        logger.info("User {} ({}) refused channel: {}", nickname, userId, refusal)
                                        send(Frame.Text(PttJson.encodeToString<ControlMessage>(ControlMessage.SystemAlert(refusal))))
                                        continue
                                    }
                                    currentChannelId = message.channelId
                                    logger.info("User {} ({}) joined channel {}", nickname, userId, message.channelId)

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
                                    logger.info("User {} ({}) left channel {}", nickname, userId, message.channelId)
                                    channel?.removeParticipant(userId)
                                    channelRegistry.scheduleCleanupIfEmpty(message.channelId)
                                    channelRegistry.broadcastActiveChannels()
                                }

                                is ControlMessage.StartSpeaking -> {
                                    logger.debug("User {} requested the floor on channel {}", userId, message.channelId)
                                    val channel = channelRegistry.getChannel(message.channelId)
                                    if (channel != null) {
                                        val granted = channel.requestFloor(userId)
                                        logger.debug("Floor granted to {}: {}", userId, granted)
                                        if (!granted) {
                                            val json =
                                                PttJson.encodeToString<ControlMessage>(
                                                    ControlMessage.FloorDenied(message.channelId, "Alguém já está falando"),
                                                )
                                            send(Frame.Text(json))
                                        }
                                    } else {
                                        logger.warn("Channel {} not found while requesting the floor", message.channelId)
                                    }
                                }

                                is ControlMessage.StopSpeaking -> {
                                    logger.debug("User {} released the floor on channel {}", userId, message.channelId)
                                    val channel = channelRegistry.getChannel(message.channelId)
                                    channel?.releaseFloor(userId)
                                }

                                else -> {} // ParticipantList, SpeakerChanged, FloorDenied are Server -> Client
                            }
                        } catch (e: Exception) {
                            logger.warn("Invalid control message: {}", e.message)
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
                                logger.warn("Failed to dispatch audio broadcast: {}", e.message)
                            }
                        }
                    }

                    else -> {}
                }
            }
        } finally {
            val channel = currentChannelId?.let { channelRegistry.getChannel(it) }
            val nickname = currentUserId?.let { channel?.getParticipant(it)?.nickname } ?: "Desconhecido"
            logger.info("Client disconnected: {} ({})", nickname, currentUserId)
            if (currentUserId != null && currentChannelId != null) {
                channel?.removeParticipant(currentUserId)
                channelRegistry.scheduleCleanupIfEmpty(currentChannelId)
            }
            channelRegistry.removeGlobalConnection(this)
        }
    }
}
