@file:Suppress("TooGenericExceptionCaught", "SwallowedException")

package com.pttlan.server.channel

import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.core.network.protocol.ParticipantDto
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

data class Participant(
    val userId: String,
    val nickname: String,
    val session: DefaultWebSocketServerSession,
    var isSpeaking: Boolean = false,
) {
    fun toDto() =
        ParticipantDto(
            userId = userId,
            nickname = nickname,
            isSpeaking = isSpeaking,
        )
}

class PttChannel(
    val id: String,
) {
    private val participants = mutableMapOf<String, Participant>()
    private val mutex = Mutex()

    val participantCount: Int
        get() = participants.size

    suspend fun addParticipant(participant: Participant) {
        mutex.withLock {
            participants[participant.userId] = participant
        }
        broadcastParticipantList()
    }

    suspend fun removeParticipant(userId: String) {
        releaseFloorIfHeldBy(userId)
        mutex.withLock {
            participants.remove(userId)
        }
        broadcastParticipantList()
    }

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

    suspend fun broadcastBinary(
        frame: Frame.Binary,
        senderUserId: String,
    ) {
        val snapshot =
            mutex.withLock {
                if (currentSpeakerId != senderUserId) return
                participants.values.toList()
            }

        snapshot.filter { it.userId != senderUserId }.forEach {
            try {
                it.session.send(Frame.Binary(true, frame.data.copyOf()))
            } catch (_: Exception) {
                // Ignore
            }
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
                    broadcast(ControlMessage.SpeakerChanged(id, userId, nickname, true))
                }
            }.first

    suspend fun releaseFloor(userId: String) {
        val nickname =
            mutex.withLock {
                if (currentSpeakerId == userId) {
                    currentSpeakerId = null
                    val p = participants[userId]
                    p?.isSpeaking = false
                    p?.nickname ?: "Desconhecido"
                } else {
                    return // Not the current speaker, ignore
                }
            }
        broadcast(ControlMessage.SpeakerChanged(id, userId, nickname, false))
    }

    suspend fun releaseFloorIfHeldBy(userId: String) {
        val held = mutex.withLock { currentSpeakerId == userId }
        if (held) releaseFloor(userId)
    }

    suspend fun getParticipant(userId: String): Participant? = mutex.withLock { participants[userId] }
}
