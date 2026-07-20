package com.pttlan.core.network

import com.pttlan.core.network.protocol.AudioEnvelope
import com.pttlan.core.network.protocol.ControlMessage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@kotlinx.serialization.Serializable
data class LoginRequest(
    val nickname: String,
    val deviceId: String,
)

@kotlinx.serialization.Serializable
data class LoginResponse(
    val token: String,
)

class PttWebSocketClient(
    private val httpClient: HttpClient,
) {
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

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    suspend fun login(
        host: String,
        port: Int,
        isLocal: Boolean,
        nickname: String,
        deviceId: String,
    ): String {
        val cleanHost = normalizeHost(host)
        val url = "https://$cleanHost:$port/api/auth/login"
        val response: LoginResponse =
            httpClient
                .post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(nickname, deviceId))
                }.body()
        return response.token
    }

    suspend fun connect(
        host: String,
        port: Int,
        isLocal: Boolean,
        token: String,
    ) {
        shouldReconnect = true
        var isFirstAttempt = true
        var backoffMs = 1000L
        val maxBackoffMs = 30000L

        while (shouldReconnect) {
            try {
                val cleanHost = normalizeHost(host)
                sessionMutex.withLock {
                    if (session != null) return@withLock
                    println("PttWebSocketClient: Tentando conectar a wss://$cleanHost:$port/ws com token")
                    val timeout = if (isLocal) 5.seconds else 15.seconds
                    session =
                        withTimeout(timeout) {
                            httpClient.webSocketSession("wss://$cleanHost:$port/ws?token=$token")
                        }
                }

                println("PttWebSocketClient: Conectado com sucesso!")
                isFirstAttempt = false
                // Reset backoff on successful connection
                backoffMs = 1000L
                _isConnected.value = true

                // Handle incoming frames
                val ws = sessionMutex.withLock { session } ?: break
                for (frame in ws.incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            try {
                                val message = Json.decodeFromString<ControlMessage>(text)
                                _controlMessages.emit(message)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        is Frame.Binary -> {
                            try {
                                val buffer = okio.Buffer().write(frame.data)
                                val envLen = buffer.readInt()
                                val envJson = buffer.readByteArray(envLen.toLong()).decodeToString()
                                val envelope = Json.decodeFromString<AudioEnvelope>(envJson)
                                val chunk = buffer.readByteArray()
                                _audioChunks.emit(Pair(envelope, chunk))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                _audioChunks.emit(Pair(null, frame.data))
                            }
                        }
                        else -> {}
                    }
                }

                val closeReason = withTimeoutOrNull(1.seconds) { ws.closeReason.await() }
                if (closeReason?.message == "Nome já em uso") {
                    shouldReconnect = false
                    throw IllegalStateException("Nome já em uso. Por favor, escolha outro.")
                }
            } catch (e: Exception) {
                println("PttWebSocketClient: Falha ao conectar: ${e.message}")
                e.printStackTrace()
                if (isFirstAttempt) {
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
                val jitter = (Random.nextDouble(0.8, 1.2) * backoffMs).toLong()
                delay(jitter.milliseconds)
                backoffMs = minOf(backoffMs * 2, maxBackoffMs)
            }
        }
    }

    suspend fun disconnect() {
        shouldReconnect = false
        sessionMutex.withLock {
            session?.close()
            session = null
        }
    }

    suspend fun sendControlMessage(message: ControlMessage) {
        try {
            val json = Json.encodeToString(message)
            session?.send(Frame.Text(json))
        } catch (e: Exception) {
            e.printStackTrace()
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
            val envJson = Json.encodeToString(envelope).encodeToByteArray()
            val envLen = envJson.size

            val buffer = okio.Buffer()
            buffer.writeInt(envLen)
            buffer.write(envJson)
            buffer.write(chunk)

            session?.send(Frame.Binary(true, buffer.readByteArray()))
        } catch (e: Exception) {
            e.printStackTrace()
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
