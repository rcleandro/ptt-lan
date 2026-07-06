package com.pttlan.core.network

import com.pttlan.core.network.protocol.AudioEnvelope
import com.pttlan.core.network.protocol.ControlMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class PttWebSocketClient(
    private val httpClient: HttpClient,
) {
    private var session: DefaultClientWebSocketSession? = null
    private val sessionMutex = Mutex()

    private val _controlMessages = MutableSharedFlow<ControlMessage>()
    val controlMessages: Flow<ControlMessage> = _controlMessages.asSharedFlow()

    private val _audioChunks = MutableSharedFlow<Pair<AudioEnvelope?, ByteArray>>()
    val audioChunks: Flow<Pair<AudioEnvelope?, ByteArray>> = _audioChunks.asSharedFlow()

    private var shouldReconnect = false
    private var connectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    suspend fun connect(
        host: String,
        port: Int,
    ) {
        shouldReconnect = true
        var backoffMs = 1000L
        val maxBackoffMs = 30000L

        while (shouldReconnect) {
            try {
                sessionMutex.withLock {
                    if (session != null) return@withLock
                    session = httpClient.webSocketSession("wss://$host:$port/ws")
                }

                // Reset backoff on successful connection
                backoffMs = 1000L

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
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
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
        val json = Json.encodeToString(message)
        session?.send(Frame.Text(json))
    }

    suspend fun sendAudioChunk(
        envelope: AudioEnvelope,
        chunk: ByteArray,
    ) {
        val envJson = Json.encodeToString(envelope).encodeToByteArray()
        val envLen = envJson.size

        val buffer = okio.Buffer()
        buffer.writeInt(envLen)
        buffer.write(envJson)
        buffer.write(chunk)

        session?.send(Frame.Binary(true, buffer.readByteArray()))
    }
}
