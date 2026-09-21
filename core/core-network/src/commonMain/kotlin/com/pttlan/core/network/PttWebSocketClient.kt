package com.pttlan.core.network

import co.touchlab.kermit.Logger
import com.pttlan.core.network.protocol.AudioEnvelope
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.core.network.protocol.LoginRequest
import com.pttlan.core.network.protocol.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The server closed the session on purpose (name taken, invalid token). Retrying will not help. */
class ServerRefusedException(
    val reason: String,
) : IllegalStateException(reason)

/** Default from the technical plan: give up after 10 failed reconnection attempts. */
const val DEFAULT_MAX_RECONNECT_ATTEMPTS = 10

class PttWebSocketClient(
    private val httpClient: HttpClient,
    private val maxReconnectAttempts: Int = DEFAULT_MAX_RECONNECT_ATTEMPTS,
) {
    private val logger = Logger.withTag("network")
    private var session: DefaultClientWebSocketSession? = null
    private val sessionMutex = Mutex()

    private val _controlMessages =
        MutableSharedFlow<ControlMessage>(
            extraBufferCapacity = 100,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val controlMessages: Flow<ControlMessage> = _controlMessages.asSharedFlow()

    private val _audioChunks =
        MutableSharedFlow<Pair<AudioEnvelope?, ByteArray>>(
            extraBufferCapacity = 100,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val audioChunks: Flow<Pair<AudioEnvelope?, ByteArray>> = _audioChunks.asSharedFlow()

    private var shouldReconnect = false

    /** Last channel joined, replayed after a reconnection so the user stays where they were. */
    private var lastJoinChannel: ControlMessage.JoinChannel? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    /** Why the server closed the last session, when it said so. Null when the drop had no stated reason. */
    var lastCloseReason: String? = null
        private set

    suspend fun login(
        host: String,
        port: Int,
        isLocal: Boolean,
        nickname: String,
        deviceId: String,
        pin: String? = null,
    ): LoginResponse {
        val cleanHost = normalizeHost(host)
        val url = "https://$cleanHost:$port/api/auth/login"
        val response =
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(nickname, deviceId, pin))
            }
        // The only 401 at login is a room PIN that does not match (host mode, 24.3)
        check(response.status != HttpStatusCode.Unauthorized) { "PIN da sala incorreto" }
        return response.body()
    }

    suspend fun connect(
        host: String,
        port: Int,
        isLocal: Boolean,
        token: String,
    ) {
        shouldReconnect = true
        var isFirstAttempt = true
        var hadConnected = false
        var failedAttempts = 0
        var backoffMs = 1000L
        val maxBackoffMs = 30000L

        while (shouldReconnect) {
            try {
                val cleanHost = normalizeHost(host)
                sessionMutex.withLock {
                    if (session != null) return@withLock
                    logger.d { "Connecting to wss://$cleanHost:$port/ws" }
                    val timeout = if (isLocal) 5.seconds else 15.seconds
                    session =
                        withTimeout(timeout) {
                            httpClient.webSocketSession(
                                "wss://$cleanHost:$port/ws" +
                                    "?token=$token&version=$APP_VERSION&protocol=$PROTOCOL_VERSION",
                            )
                        }
                }

                logger.i { "Connected" }
                lastCloseReason = null
                if (hadConnected) {
                    lastJoinChannel?.let { sendControlMessage(it) }
                }
                isFirstAttempt = false
                hadConnected = true
                // Reset backoff and the attempt budget on a successful connection
                backoffMs = 1000L
                failedAttempts = 0
                _isConnected.value = true

                // Handle incoming frames
                val ws = sessionMutex.withLock { session } ?: break
                for (frame in ws.incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            try {
                                val message = PttJson.decodeFromString<ControlMessage>(text)
                                _controlMessages.emit(message)
                            } catch (e: Exception) {
                                logger.w(e) { "Invalid control message" }
                            }
                        }

                        is Frame.Binary -> {
                            try {
                                val buffer = okio.Buffer().write(frame.data)
                                val envLen = buffer.readInt()
                                val envJson = buffer.readByteArray(envLen.toLong()).decodeToString()
                                val envelope = PttJson.decodeFromString<AudioEnvelope>(envJson)
                                val chunk = buffer.readByteArray()
                                _audioChunks.emit(Pair(envelope, chunk))
                            } catch (e: Exception) {
                                logger.w(e) { "Invalid audio envelope" }
                                _audioChunks.emit(Pair(null, frame.data))
                            }
                        }

                        else -> {}
                    }
                }

                // A policy close is the server refusing this client (name taken, bad token): retrying is pointless
                // and the reason has to reach the UI instead of a generic "desconectado".
                val closeReason = withTimeoutOrNull(1.seconds) { ws.closeReason.await() }
                if (closeReason?.knownReason == CloseReason.Codes.VIOLATED_POLICY) {
                    val reason = closeReason.message.takeIf { it.isNotBlank() } ?: "Conexão recusada pelo servidor"
                    lastCloseReason = reason
                    shouldReconnect = false
                    throw ServerRefusedException(reason)
                }
            } catch (e: Exception) {
                logger.w(e) { "Connection lost" }
                // A refusal is rethrown even mid-session: otherwise the reason dies here and the UI only ever
                // learns that the connection dropped.
                if (isFirstAttempt || e is ServerRefusedException) {
                    shouldReconnect = false
                    throw e
                }
            } finally {
                _isConnected.value = false
                sessionMutex.withLock {
                    session?.close()
                    session = null
                }
            }

            if (shouldReconnect) {
                failedAttempts++
                if (failedAttempts >= maxReconnectAttempts) {
                    logger.w { "Giving up after $maxReconnectAttempts failed reconnection attempts" }
                    shouldReconnect = false
                    break
                }
                val jitter = (Random.nextDouble(0.8, 1.2) * backoffMs).toLong()
                delay(jitter.milliseconds)
                backoffMs = minOf(backoffMs * 2, maxBackoffMs)
            }
        }
    }

    suspend fun disconnect() {
        shouldReconnect = false
        lastJoinChannel = null
        lastCloseReason = null
        sessionMutex.withLock {
            session?.close()
            session = null
        }
    }

    suspend fun sendControlMessage(message: ControlMessage) {
        when (message) {
            is ControlMessage.JoinChannel -> {
                lastJoinChannel = message
            }

            is ControlMessage.LeaveChannel -> {
                if (lastJoinChannel?.channelId == message.channelId) lastJoinChannel = null
            }

            else -> {}
        }
        try {
            val json = PttJson.encodeToString(message)
            session?.send(Frame.Text(json))
        } catch (e: Exception) {
            logger.w(e) { "Failed to send over the WebSocket" }
            _isConnected.value = false
            try {
                sessionMutex.withLock {
                    val s = session
                    session = null
                    s?.close()
                }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun sendAudioChunk(
        envelope: AudioEnvelope,
        chunk: ByteArray,
    ) {
        try {
            val envJson = PttJson.encodeToString(envelope).encodeToByteArray()
            val envLen = envJson.size

            val buffer = okio.Buffer()
            buffer.writeInt(envLen)
            buffer.write(envJson)
            buffer.write(chunk)

            session?.send(Frame.Binary(true, buffer.readByteArray()))
        } catch (e: Exception) {
            logger.w(e) { "Failed to send over the WebSocket" }
            _isConnected.value = false
            try {
                sessionMutex.withLock {
                    val s = session
                    session = null
                    s?.close()
                }
            } catch (_: Exception) {
            }
        }
    }
}
