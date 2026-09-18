
package com.pttlan.server.channel

import com.pttlan.core.network.PttJson
import com.pttlan.core.network.protocol.ActiveChannelDto
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.server.routing.DashboardChannelDto
import com.pttlan.server.routing.DashboardLogEventDto
import com.pttlan.server.routing.DashboardParticipantDto
import com.pttlan.server.routing.SpeakerTimeDto
import com.pttlan.server.routing.TimeSeriesPointDto
import com.sun.management.OperatingSystemMXBean
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

private const val CLEANUP_DELAY_MS = 5 * 60 * 1000L
private const val MAX_LOGS = 100
private const val MS_PER_SECOND = 1000L
private const val TIME_SERIES_CUTOFF_MINUTES = 30
private const val MS_PER_MINUTE = 60_000L

/** A live WebSocket connection: the nickname it holds and the device that owns it. */
private data class GlobalConnection(
    val nickname: String,
    val deviceId: String,
)

private class MutableTimeSeriesPoint(
    val timestampMs: Long,
    var bytesTransferred: Long = 0,
    var pttStarts: Int = 0,
    var slowConnections: Int = 0,
    var maxCpuLoad: Double = 0.0,
    var maxMemoryUsedPercent: Double = 0.0,
)

@Suppress("TooManyFunctions")
class ChannelRegistry(
    private val floorIdleTimeoutMs: Long = DEFAULT_FLOOR_IDLE_TIMEOUT_MS,
    private val maxSpeechDurationMs: Long = DEFAULT_MAX_SPEECH_DURATION_MS,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val channels = ConcurrentHashMap<String, PttChannel>()
    private val globalConnections = ConcurrentHashMap<DefaultWebSocketServerSession, GlobalConnection>()
    private val cleanupJobs = ConcurrentHashMap<String, Job>()
    private val accumulatedSpeakerTime = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(dispatcher)

    private val logMutex = Mutex()
    private val recentLogs = ArrayDeque<DashboardLogEventDto>()

    private val timeSeriesMutex = Mutex()
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

    /**
     * Registers a connection, keeping nicknames unique **between devices**. A reconnection from the same
     * `deviceId` replaces its own previous session — which may still be open because the server only notices
     * a silent drop at the ping timeout (~20s) — instead of refusing the user their own name.
     */
    fun addGlobalConnection(
        session: DefaultWebSocketServerSession,
        nickname: String,
        deviceId: String,
    ): Boolean {
        val sameNickname =
            globalConnections.entries.filter { it.value.nickname.equals(nickname, ignoreCase = true) }
        val fromAnotherDevice = sameNickname.any { it.value.deviceId != deviceId }
        if (fromAnotherDevice) {
            return false
        }

        sameNickname.forEach { (staleSession, _) ->
            globalConnections.remove(staleSession)
            scope.launch {
                staleSession.close(CloseReason(CloseReason.Codes.NORMAL, "Sessão substituída por uma nova conexão"))
            }
        }

        globalConnections[session] = GlobalConnection(nickname, deviceId)
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
                    floorIdleTimeoutMs = floorIdleTimeoutMs,
                    maxSpeechDurationMs = maxSpeechDurationMs,
                    scope = scope,
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

    @Suppress("MagicNumber")
    suspend fun getTimeSeries(): List<TimeSeriesPointDto> {
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val rawCpuLoad =
            if (osBean is OperatingSystemMXBean) {
                osBean.processCpuLoad * 100.0
            } else {
                0.0
            }
        val cpuLoad = rawCpuLoad.takeIf { it >= 0.0 && !it.isNaN() }?.coerceAtMost(100.0) ?: 0.0

        val totalMemory = Runtime.getRuntime().totalMemory()
        val freeMemory = Runtime.getRuntime().freeMemory()
        val maxMemory = Runtime.getRuntime().maxMemory()
        val rawMem = ((totalMemory - freeMemory).toDouble() / maxMemory) * 100.0
        val memoryUsedPercent = rawMem.takeIf { !it.isNaN() }?.coerceIn(0.0, 100.0) ?: 0.0

        val currentPoint = getOrCreateCurrentMetric()
        currentPoint.maxCpuLoad = cpuLoad
        currentPoint.maxMemoryUsedPercent = memoryUsedPercent

        val cutoff = (System.currentTimeMillis() / MS_PER_MINUTE) - TIME_SERIES_CUTOFF_MINUTES
        return timeSeriesMutex.withLock {
            timeSeriesMetrics.keys.removeAll { it < cutoff }
            timeSeriesMetrics.values
                .map {
                    TimeSeriesPointDto(
                        it.timestampMs,
                        it.bytesTransferred,
                        it.pttStarts,
                        it.slowConnections,
                        it.maxCpuLoad,
                        it.maxMemoryUsedPercent,
                    )
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
                            ipAddress = p.ipAddress,
                            appVersion = p.appVersion,
                            pingMs = p.pingMs,
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

    suspend fun resetServer() {
        globalConnections.keys.forEach {
            try {
                it.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server Restarting"))
            } catch (_: Exception) {
            }
        }

        channels.values.forEach { channel ->
            val participants = channel.getParticipantsSnapshot()
            participants.forEach {
                try {
                    it.session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server Restarting"))
                } catch (_: Exception) {
                }
            }
        }

        globalConnections.clear()
        channels.clear()
        accumulatedSpeakerTime.clear()
        cleanupJobs.values.forEach { it.cancel() }
        cleanupJobs.clear()

        logMutex.withLock {
            recentLogs.clear()
        }

        timeSeriesMutex.withLock {
            timeSeriesMetrics.clear()
        }

        getOrCreateChannel("Geral")
    }

    suspend fun broadcastGlobalAlert(message: String) {
        val alert: ControlMessage = ControlMessage.SystemAlert(message)
        // Typed as the sealed interface on purpose: serializing the concrete class drops the "type"
        // discriminator and no client can decode the frame.
        val json = PttJson.encodeToString(alert)
        globalConnections.keys.forEach {
            try {
                it.send(Frame.Text(json))
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun broadcastActiveChannels() {
        val activeChannels =
            channels.values
                .map { ActiveChannelDto(it.id, it.participantCount) }
        // Optionally filter empty ones if you don't want them visible, but they should be visible until deleted.

        val message: ControlMessage = ControlMessage.ActiveChannelsList(activeChannels)
        val json = PttJson.encodeToString<ControlMessage>(message)

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
