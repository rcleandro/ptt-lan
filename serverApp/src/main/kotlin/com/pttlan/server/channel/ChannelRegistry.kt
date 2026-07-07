@file:Suppress("TooGenericExceptionCaught", "SwallowedException", "MagicNumber")

package com.pttlan.server.channel

import com.pttlan.core.network.protocol.ActiveChannelDto
import com.pttlan.core.network.protocol.ControlMessage
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

class ChannelRegistry {
    private val channels = ConcurrentHashMap<String, PttChannel>()
    private val globalConnections = ConcurrentHashMap<DefaultWebSocketServerSession, String>()
    private val cleanupJobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default)

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
        val channel = channels[channelId] ?: return
        if (channel.participantCount == 0) {
            cleanupJobs[channelId]?.cancel()
            cleanupJobs[channelId] =
                scope.launch {
                    delay((5 * 60 * 1000L).milliseconds) // 5 minutes
                    if (channel.participantCount == 0) {
                        channels.remove(channelId)
                        broadcastActiveChannels()
                    }
                }
        }
        broadcastActiveChannels()
    }

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
