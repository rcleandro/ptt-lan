package com.pttlan.server

import com.pttlan.core.network.protocol.ControlMessage
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerIntegrationTest {

    @Test
    fun testPttFloorControl() = testApplication {
        application {
            module()
        }

        val client1 = createClient {
            install(WebSockets)
        }
        val client2 = createClient {
            install(WebSockets)
        }

        client1.webSocket("/ws?nickname=Client1") {
            val session1 = this
            
            // Client1 joins
            val join1 = ControlMessage.JoinChannel("channel-1", "Client1", "u1")
            session1.send(Frame.Text(Json.encodeToString<ControlMessage>(join1)))
            
            client2.webSocket("/ws?nickname=Client2") {
                val session2 = this
                
                // Client2 joins
                val join2 = ControlMessage.JoinChannel("channel-1", "Client2", "u2")
                session2.send(Frame.Text(Json.encodeToString<ControlMessage>(join2)))

                // Client1 requests floor
                val startSpeaking1 = ControlMessage.StartSpeaking("channel-1", "u1")
                session1.send(Frame.Text(Json.encodeToString<ControlMessage>(startSpeaking1)))

                // Read from session2 to get SpeakerChanged event confirming Client1 is speaking, so we know they have the floor
                // Actually wait, let's just make Client2 try to speak
                val startSpeaking2 = ControlMessage.StartSpeaking("channel-1", "u2")
                session2.send(Frame.Text(Json.encodeToString<ControlMessage>(startSpeaking2)))

                // Client2 should receive FloorDenied
                var receivedFloorDenied = false
                
                // We'll read incoming frames from Client2 until we get FloorDenied
                try {
                    val frame = session2.incoming.consumeAsFlow().filterIsInstance<Frame.Text>().first { frame ->
                        val text = frame.readText()
                        val msg = Json.decodeFromString<ControlMessage>(text)
                        msg is ControlMessage.FloorDenied
                    }
                    val msg = Json.decodeFromString<ControlMessage>(frame.readText()) as ControlMessage.FloorDenied
                    assertEquals("channel-1", msg.channelId)
                    receivedFloorDenied = true
                } catch (e: Exception) {
                    // Closed or timeout
                }

                // Stop speaking for client 1 to clean up
                session1.send(Frame.Text(Json.encodeToString<ControlMessage>(ControlMessage.StopSpeaking("channel-1", "u1"))))
                
                // Close sessions
                session2.close()
                session1.close()

                assertTrue(receivedFloorDenied, "Client2 should have received FloorDenied message")
            }
        }
    }
}
