package com.pttlan.server.channel

import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.core.network.protocol.ParticipantDto
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val logger = LoggerFactory.getLogger(PttChannel::class.java)

private const val SLOW_CONNECTION_THRESHOLD_MS = 100L

/** Audio packets buffered per listener (~1s of 20ms frames) before the oldest ones are dropped. */
private const val OUTBOUND_BUFFER = 50

/** No audio for this long and the floor goes back, so a speaker that vanishes does not block the channel. */
const val DEFAULT_FLOOR_IDLE_TIMEOUT_MS = 2_000L

/** Hard ceiling for a single turn, in case a device keeps streaming unattended. */
const val DEFAULT_MAX_SPEECH_DURATION_MS = 60_000L

data class Participant(
    val userId: String,
    val nickname: String,
    val session: DefaultWebSocketServerSession,
    var isSpeaking: Boolean = false,
    val ipAddress: String = "Desconhecido",
    val appVersion: String = "Desconhecida",
    var pingMs: Long = 0L,
) {
    /**
     * Per-listener audio queue. The broadcast only offers packets here, so a slow connection drops its own
     * backlog instead of holding up everyone else's audio.
     */
    val outbound: Channel<ByteArray> =
        Channel(capacity = OUTBOUND_BUFFER, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    var pump: Job? = null

    fun toDto() =
        ParticipantDto(
            userId = userId,
            nickname = nickname,
            isSpeaking = isSpeaking,
        )
}

@Suppress("TooManyFunctions")
class PttChannel(
    val id: String,
    private val floorIdleTimeoutMs: Long = DEFAULT_FLOOR_IDLE_TIMEOUT_MS,
    private val maxSpeechDurationMs: Long = DEFAULT_MAX_SPEECH_DURATION_MS,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val onLog: suspend (participantName: String, eventType: String) -> Unit = { _, _ -> },
    private val onSpeakDuration: suspend (participantName: String, durationMs: Long) -> Unit = { _, _ -> },
    private val onMetric: suspend (bytes: Long, slowCount: Int) -> Unit = { _, _ -> },
) {
    private val participants = mutableMapOf<String, Participant>()
    private val mutex = Mutex()

    // Written by the per-listener pumps and drained by the broadcast, so they have to be atomic
    private val bytesTransferred = AtomicLong()
    private val slowSends = AtomicInteger()

    val participantCount: Int
        get() = participants.size

    suspend fun addParticipant(participant: Participant) {
        mutex.withLock {
            participants[participant.userId] = participant
        }
        participant.pump = startOutboundPump(participant)
        onLog(participant.nickname, "JOIN")
        broadcastParticipantList()
    }

    suspend fun removeParticipant(userId: String) {
        releaseFloorIfHeldBy(userId)
        val p = mutex.withLock { participants.remove(userId) }
        p?.outbound?.close()
        p?.pump?.cancel()
        p?.let { onLog(it.nickname, "LEAVE") }
        broadcastParticipantList()
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend fun broadcast(message: ControlMessage) {
        val json = Json.encodeToString(message)
        val snapshot = mutex.withLock { participants.values.toList() }

        snapshot.forEach {
            try {
                it.session.send(Frame.Text(json))
            } catch (_: Exception) {
                // Ignore, will be cleaned up on disconnect
            }
        }
    }

    /** Drains one listener's queue, one packet at a time, and times the slow sends. */
    @Suppress("TooGenericExceptionCaught")
    private fun startOutboundPump(participant: Participant): Job =
        scope.launch {
            for (data in participant.outbound) {
                val startMs = System.currentTimeMillis()
                try {
                    participant.session.send(Frame.Binary(true, data))
                    bytesTransferred.addAndGet(data.size.toLong())
                    val elapsed = System.currentTimeMillis() - startMs
                    if (elapsed > SLOW_CONNECTION_THRESHOLD_MS) {
                        slowSends.incrementAndGet()
                        logger.debug(
                            "PttChannel[{}]: envio para {} demorou {}ms (conexão lenta?)",
                            id,
                            participant.nickname,
                            elapsed,
                        )
                    }
                } catch (e: Exception) {
                    logger.debug(
                        "PttChannel[{}]: falha ao enviar áudio para {}: {}",
                        id,
                        participant.userId,
                        e.message,
                    )
                }
            }
        }

    /**
     * Queues the packet for every listener and returns. The frame buffer is shared instead of copied per
     * recipient — nothing writes to it after this point — and a listener that cannot keep up only loses
     * its own oldest packets.
     */
    suspend fun broadcastBinary(
        frame: Frame.Binary,
        senderUserId: String,
    ) {
        lastAudioAtMs = System.currentTimeMillis()
        val targets =
            mutex.withLock {
                if (currentSpeakerId != senderUserId) {
                    logger.debug(
                        "PttChannel[{}]: descartando áudio de {}, o speaker atual é {}",
                        id,
                        senderUserId,
                        currentSpeakerId,
                    )
                    return
                }
                participants.values.filter { it.userId != senderUserId }
            }

        if (targets.isEmpty()) {
            return
        }

        val data = frame.data
        logger.debug(
            "PttChannel[{}]: áudio de {} bytes de {} para {} ouvintes",
            id,
            data.size,
            senderUserId,
            targets.size,
        )
        targets.forEach { it.outbound.trySend(data) }

        val bytes = bytesTransferred.getAndSet(0)
        val slow = slowSends.getAndSet(0)
        if (bytes > 0 || slow > 0) {
            onMetric(bytes, slow)
        }
    }

    suspend fun broadcastParticipantList() {
        val snapshot = mutex.withLock { participants.values.toList() }
        val msg =
            ControlMessage.ParticipantList(
                channelId = id,
                participants = snapshot.map { it.toDto() },
            )
        broadcast(msg)
    }

    var currentSpeakerId: String? = null
        private set

    private var speakerStartTime: Long = 0

    @Volatile
    private var lastAudioAtMs: Long = 0

    private var floorWatchdog: Job? = null

    /**
     * Releases the floor when the speaker goes quiet (dropped connection, app killed) or talks past the
     * per-turn ceiling. Without it the floor is only freed by `StopSpeaking` or by the ping timeout (~35s).
     */
    private fun startFloorWatchdog(userId: String) {
        floorWatchdog?.cancel()
        floorWatchdog =
            scope.launch {
                while (isActive) {
                    val now = System.currentTimeMillis()
                    val idleLeftMs = floorIdleTimeoutMs - (now - lastAudioAtMs)
                    val speechLeftMs = maxSpeechDurationMs - (now - speakerStartTime)
                    if (idleLeftMs <= 0 || speechLeftMs <= 0) {
                        // Cleared first so releaseFloor does not cancel the coroutine running it
                        floorWatchdog = null
                        releaseFloor(userId)
                        return@launch
                    }
                    delay(minOf(idleLeftMs, speechLeftMs))
                }
            }
    }

    suspend fun requestFloor(userId: String): Boolean =
        mutex
            .withLock {
                if (currentSpeakerId == null || currentSpeakerId == userId) {
                    val participant = participants[userId] ?: return@withLock Pair(false, "")
                    currentSpeakerId = userId
                    participant.isSpeaking = true
                    Pair(true, participant.nickname)
                } else {
                    Pair(false, "")
                }
            }.also { (granted, nickname) ->
                if (granted) {
                    speakerStartTime = System.currentTimeMillis()
                    lastAudioAtMs = speakerStartTime
                    startFloorWatchdog(userId)
                    onLog(nickname, "START_SPEAKING")
                    broadcast(ControlMessage.SpeakerChanged(id, userId, nickname, true))
                }
            }.first

    suspend fun releaseFloor(userId: String) {
        floorWatchdog?.cancel()
        floorWatchdog = null
        val (nickname, durationMs) =
            mutex.withLock {
                if (currentSpeakerId == userId) {
                    currentSpeakerId = null
                    val p = participants[userId]
                    p?.isSpeaking = false
                    val duration = System.currentTimeMillis() - speakerStartTime
                    Pair(p?.nickname ?: "Desconhecido", duration)
                } else {
                    return // Not the current speaker, ignore
                }
            }
        onSpeakDuration(nickname, durationMs)
        onLog(nickname, "STOP_SPEAKING")
        broadcast(ControlMessage.SpeakerChanged(id, userId, nickname, false))
    }

    suspend fun releaseFloorIfHeldBy(userId: String) {
        val held = mutex.withLock { currentSpeakerId == userId }
        if (held) releaseFloor(userId)
    }

    suspend fun getParticipant(userId: String): Participant? = mutex.withLock { participants[userId] }

    suspend fun getParticipantsSnapshot(): List<Participant> = mutex.withLock { participants.values.toList() }
}
