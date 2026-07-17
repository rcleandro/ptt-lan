
package com.pttlan.server.channel

import com.pttlan.core.network.protocol.ActiveChannelDto
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.server.routing.DashboardChannelDto
import com.pttlan.server.routing.DashboardLogEventDto
import com.pttlan.server.routing.DashboardParticipantDto
import com.pttlan.server.routing.SpeakerTimeDto
import com.pttlan.server.routing.TimeSeriesPointDto
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

private const val CLEANUP_DELAY_MS = 5 * 60 * 1000L
private const val MAX_LOGS = 100
private const val MS_PER_SECOND = 1000L
private const val TIME_SERIES_CUTOFF_MINUTES = 30
private const val MS_PER_MINUTE = 60_000L

private class MutableTimeSeriesPoint(
    val timestampMs: Long,
    var bytesTransferred: Long = 0,
    var pttStarts: Int = 0,
    var slowConnections: Int = 0,
)

@Suppress("TooManyFunctions")
class ChannelRegistry {
    private val channels = ConcurrentHashMap<String, PttChannel>()
    private val globalConnections = ConcurrentHashMap<DefaultWebSocketServerSession, String>()
    private val cleanupJobs = ConcurrentHashMap<String, Job>()
    private val accumulatedSpeakerTime = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(Dispatchers.Default)

    private val logMutex = kotlinx.coroutines.sync.Mutex()
    private val recentLogs = kotlin.collections.ArrayDeque<DashboardLogEventDto>()

    private val timeSeriesMutex = kotlinx.coroutines.sync.Mutex()
    private val timeSeriesMetrics = ConcurrentHashMap<Long, MutableTimeSeriesPoint>()

    private suspend fun getOrCreateCurrentMetric(): MutableTimeSeriesPoint {
        val currentMinute = System.currentTimeMillis() / MS_PER_MINUTE
        return timeSeriesMutex.withLock {
            timeSeriesMetrics.getOrPut(currentMinute) { MutableTimeSeriesPoint(currentMinute * MS_PER_MINUTE) }
        }
    }

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
        val channel =
            channels.getOrPut(channelId) {
                PttChannel(
                    id = channelId,
                    onLog = { participantName, eventType ->
                        addLog(channelId, participantName, eventType)
                        if (eventType == "START_SPEAKING") {
                            scope.launch {
                                val metric = getOrCreateCurrentMetric()
                                timeSeriesMutex.withLock { metric.pttStarts += 1 }
                            }
                        }
                    },
                    onSpeakDuration = { participantName, durationMs ->
                        val current = accumulatedSpeakerTime[participantName] ?: 0L
                        accumulatedSpeakerTime[participantName] = current + durationMs
                    },
                    onMetric = { bytes, slowCount ->
                        scope.launch {
                            val metric = getOrCreateCurrentMetric()
                            timeSeriesMutex.withLock {
                                metric.bytesTransferred += bytes
                                metric.slowConnections += slowCount
                            }
                        }
                    },
                )
            }
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

    private suspend fun addLog(
        channelId: String,
        participantName: String,
        eventType: String,
    ) {
        logMutex.withLock {
            val event = DashboardLogEventDto(System.currentTimeMillis(), channelId, participantName, eventType)
            recentLogs.addFirst(event)
            if (recentLogs.size > MAX_LOGS) {
                recentLogs.removeLast()
            }
        }
    }

    suspend fun getRecentLogs(): List<DashboardLogEventDto> = logMutex.withLock { recentLogs.toList() }

    @Suppress("MaxLineLength")
    fun getSpeakerTimes(): List<SpeakerTimeDto> = accumulatedSpeakerTime.map { SpeakerTimeDto(it.key, it.value / MS_PER_SECOND) }

    suspend fun getTimeSeries(): List<TimeSeriesPointDto> {
        val cutoff = (System.currentTimeMillis() / MS_PER_MINUTE) - TIME_SERIES_CUTOFF_MINUTES
        return timeSeriesMutex.withLock {
            timeSeriesMetrics.keys.removeAll { it < cutoff }
            timeSeriesMetrics.values
                .map {
                    TimeSeriesPointDto(it.timestampMs, it.bytesTransferred, it.pttStarts, it.slowConnections)
                }.sortedBy { it.timestampMs }
        }
    }

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

    suspend fun kickUser(
        channelId: String,
        userId: String,
    ): Boolean {
        val channel = getChannel(channelId)
        val participant = channel?.getParticipant(userId)
        if (participant != null) {
            try {
                participant.session.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Kicked by Admin"))
            } catch (_: Exception) {
                // Ignore
            }
            return true
        }
        return false
    }

    suspend fun closeChannel(channelId: String): Boolean {
        val channel = channels.remove(channelId) ?: return false
        val participants = channel.getParticipantsSnapshot()
        participants.forEach {
            try {
                it.session.close(CloseReason(CloseReason.Codes.NORMAL, "Channel closed by Admin"))
            } catch (_: Exception) {
                // Ignore
            }
        }
        broadcastActiveChannels()
        return true
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
