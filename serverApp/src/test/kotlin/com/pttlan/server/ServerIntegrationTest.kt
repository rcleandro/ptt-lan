package com.pttlan.server

import com.pttlan.core.network.protocol.ControlMessage
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerIntegrationTest {
    @Test
    @Suppress("LongMethod")
    fun testPttFloorControl() =
        testApplication {
            application {
                module()
            }

            val client1 =
                createClient {
                    install(WebSockets)
                }
            val client2 =
                createClient {
                    install(WebSockets)
                }

            val client1Connected = CompletableDeferred<Unit>()
            val client2Connected = CompletableDeferred<Unit>()
            val floorDeniedReceived = CompletableDeferred<Unit>()

            // Escopo independente para evitar o bug de UncompletedCoroutinesError de WebSockets no Ktor 3
            val testScope = CoroutineScope(Dispatchers.Default)

            val job1 =
                testScope.launch {
                    client1.webSocket("/ws?nickname=Client1") {
                        // Client1 joins
                        val join1 = ControlMessage.JoinChannel("channel-1", "Client1", "u1")
                        send(Frame.Text(Json.encodeToString<ControlMessage>(join1)))
                        client1Connected.complete(Unit)

                        // Wait for Client2 to connect and join
                        client2Connected.await()

                        // Client1 requests floor
                        val startSpeaking1 = ControlMessage.StartSpeaking("channel-1", "u1")
                        send(Frame.Text(Json.encodeToString<ControlMessage>(startSpeaking1)))

                        // Wait until Client2 receives FloorDenied
                        floorDeniedReceived.await()

                        // Stop speaking for client 1 to clean up
                        val stop1 = ControlMessage.StopSpeaking("channel-1", "u1")
                        send(Frame.Text(Json.encodeToString<ControlMessage>(stop1)))
                        close()
                    }
                }

            val job2 =
                testScope.launch {
                    client2.webSocket("/ws?nickname=Client2") {
                        // Wait for Client1 to connect and join
                        client1Connected.await()

                        // Client2 joins
                        val join2 = ControlMessage.JoinChannel("channel-1", "Client2", "u2")
                        send(Frame.Text(Json.encodeToString<ControlMessage>(join2)))
                        client2Connected.complete(Unit)

                        // Wait for Client1 to acquire the floor (SpeakerChanged)
                        var client1IsSpeaking = false
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val msg = Json.decodeFromString<ControlMessage>(text)
                                if (msg is ControlMessage.SpeakerChanged && msg.userId == "u1" && msg.isSpeaking) {
                                    client1IsSpeaking = true
                                    break
                                }
                            }
                        }
                        assertTrue(client1IsSpeaking, "Client1 should be speaking before Client2 requests floor")

                        // Client2 tries to speak while Client1 has the floor
                        val startSpeaking2 = ControlMessage.StartSpeaking("channel-1", "u2")
                        send(Frame.Text(Json.encodeToString<ControlMessage>(startSpeaking2)))

                        // Client2 should receive FloorDenied
                        var receivedFloorDenied = false

                        try {
                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    val msg = Json.decodeFromString<ControlMessage>(text)
                                    if (msg is ControlMessage.FloorDenied) {
                                        assertEquals("channel-1", msg.channelId)
                                        receivedFloorDenied = true
                                        break
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // Closed or timeout
                        }

                        floorDeniedReceived.complete(Unit)
                        close()

                        assertTrue(receivedFloorDenied, "Client2 should have received FloorDenied message")
                    }
                }

            // Aguarda a finalização de ambos os jobs no escopo de teste antes de terminar
            job1.join()
            job2.join()
            testScope.cancel()
        }
}
