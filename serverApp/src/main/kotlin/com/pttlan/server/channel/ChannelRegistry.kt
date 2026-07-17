
package com.pttlan.server.channel

import com.pttlan.core.network.protocol.ActiveChannelDto
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.server.routing.DashboardChannelDto
import com.pttlan.server.routing.DashboardParticipantDto
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

private const val CLEANUP_DELAY_MS = 5 * 60 * 1000L

class ChannelRegistry {
    private val channels = ConcurrentHashMap<String, PttChannel>()
    private val globalConnections = ConcurrentHashMap<DefaultWebSocketServerSession, String>()
    private val cleanupJobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        getOrCreateChannel("Geral")
    }

    fun addGlobalConnection(
        session: DefaultWebSocketServerSession,
        nickname: String,
    ): Boolean {
        if (globalConnections.values.any { it.equals(nickname, ignoreCase = true) }) {
            return false
        }
        globalConnections[session] = nickname
        broadcastActiveChannels()
        return true
    }

    fun removeGlobalConnection(session: DefaultWebSocketServerSession) {
        globalConnections.remove(session)
    }

    fun getOrCreateChannel(channelId: String): PttChannel {
        cleanupJobs.remove(channelId)?.cancel()
        val channel = channels.getOrPut(channelId) { PttChannel(channelId) }
        broadcastActiveChannels()
        return channel
    }

    fun getChannel(channelId: String): PttChannel? = channels[channelId]

    fun scheduleCleanupIfEmpty(channelId: String) {
        if (channelId == "Geral") {
            broadcastActiveChannels()
            return
        }
        val channel = channels[channelId] ?: return
        if (channel.participantCount == 0) {
            cleanupJobs[channelId]?.cancel()
            cleanupJobs[channelId] =
                scope.launch {
                    delay(CLEANUP_DELAY_MS.milliseconds) // 5 minutes
                    if (channel.participantCount == 0) {
                        channels.remove(channelId)
                        broadcastActiveChannels()
                    }
                }
        }
        broadcastActiveChannels()
    }

    fun getGlobalConnectionsCount(): Int = globalConnections.size

    suspend fun getActiveChannelsInfo(): List<DashboardChannelDto> =
        channels.values.map { channel ->
            DashboardChannelDto(
                id = channel.id,
                participantCount = channel.participantCount,
                currentSpeakerId = channel.currentSpeakerId,
                participants =
                    channel.getParticipantsSnapshot().map { p ->
                        DashboardParticipantDto(
                            userId = p.userId,
                            nickname = p.nickname,
                            isSpeaking = p.isSpeaking,
                        )
                    },
            )
        }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun broadcastActiveChannels() {
        val activeChannels =
            channels.values
                .map { ActiveChannelDto(it.id, it.participantCount) }
        // Optionally filter empty ones if you don't want them visible, but they should be visible until deleted.

        val message: ControlMessage = ControlMessage.ActiveChannelsList(activeChannels)
        val json = Json.encodeToString<ControlMessage>(message)

        scope.launch {
            globalConnections.keys.forEach { session ->
                try {
                    session.send(Frame.Text(json))
                } catch (_: Exception) {
                    // Ignore closed sessions
                }
            }
        }
    }
}
