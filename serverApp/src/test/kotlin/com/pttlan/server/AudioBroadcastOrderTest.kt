package com.pttlan.server

import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.server.auth.JwtConfig
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

private const val PACKETS = 30

/**
 * Phase 21.3: audio used to be dispatched with one `launch` per frame, so packets could overtake each other.
 * They are now handled in order on the session coroutine and queued per listener.
 */
class AudioBroadcastOrderTest {
    @Test
    fun deliversAudioPacketsInOrder() =
        testApplication {
            application { module() }
            val client = createClient { install(WebSockets) }
            val scope = CoroutineScope(Dispatchers.Default)
            val listenerJoined = CompletableDeferred<Unit>()
            val received = CompletableDeferred<List<Int>>()

            val listener =
                scope.launch {
                    client.webSocket("/ws?token=${JwtConfig.generateToken("u2", "Listener", "device-2")}") {
                        send(
                            Frame.Text(
                                Json.encodeToString<ControlMessage>(
                                    ControlMessage.JoinChannel("c1", "Listener", "u2"),
                                ),
                            ),
                        )
                        val order = mutableListOf<Int>()
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    if (frame.readText().contains("participant_list")) {
                                        listenerJoined.complete(Unit)
                                    }
                                }

                                is Frame.Binary -> {
                                    order += frame.data[0].toInt()
                                    if (order.size == PACKETS) {
                                        received.complete(order.toList())
                                        return@webSocket
                                    }
                                }

                                else -> {}
                            }
                        }
                    }
                }

            listenerJoined.await()

            client.webSocket("/ws?token=${JwtConfig.generateToken("u1", "Speaker", "device-1")}") {
                send(Frame.Text(Json.encodeToString<ControlMessage>(ControlMessage.JoinChannel("c1", "Speaker", "u1"))))
                send(Frame.Text(Json.encodeToString<ControlMessage>(ControlMessage.StartSpeaking("c1", "u1"))))
                repeat(PACKETS) { index ->
                    send(Frame.Binary(true, byteArrayOf(index.toByte(), 0, 0, 0)))
                }

                val order = withTimeoutOrNull(15.seconds) { received.await() }
                assertEquals(List(PACKETS) { it }, order, "packets must arrive in the order they were sent")
            }

            listener.cancel()
            scope.cancel()
        }
}
