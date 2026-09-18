package com.pttlan.server

import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.server.auth.JwtConfig
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.close
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
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ServerIntegrationTest {
    /**
     * These tests hold the floor without streaming audio, which the 20.4 watchdog would take back after 2s.
     * They are about contention and identity, so the idle timeout is pushed out of the way.
     */
    private fun ApplicationTestBuilder.serverWithoutFloorIdleTimeout() {
        environment {
            config =
                ApplicationConfig("application.conf").mergeWith(
                    MapApplicationConfig("ptt.floorIdleTimeoutMs" to "60000"),
                )
        }
    }

    @Test
    @Suppress("LongMethod")
    fun testPttFloorControl() =
        testApplication {
            serverWithoutFloorIdleTimeout()

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
                    val token1 =
                        JwtConfig
                            .generateToken("u1", "Client1", "device-1")
                    client1.webSocket("/ws?token=$token1") {
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
                    val token2 =
                        JwtConfig
                            .generateToken("u2", "Client2", "device-2")
                    client2.webSocket("/ws?token=$token2") {
                        // Wait for Client1 to connect and join
                        client1Connected.await()

                        // Client2 joins
                        val join2 = ControlMessage.JoinChannel("channel-1", "Client2", "u2")
                        send(Frame.Text(Json.encodeToString<ControlMessage>(join2)))

                        // Releasing Client1 right after `send` races the server: if Client1 takes the floor
                        // before the join is processed, the SpeakerChanged broadcast never reaches Client2.
                        // The participant list that follows the join is the server's own confirmation.
                        var client1IsSpeaking = false
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val msg = Json.decodeFromString<ControlMessage>(text)
                                if (msg is ControlMessage.ParticipantList &&
                                    msg.participants.any { it.userId == "u2" }
                                ) {
                                    client2Connected.complete(Unit)
                                }
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

    @Test
    fun serverUsesTokenIdentityAndIgnoresUserIdInMessages() =
        testApplication {
            serverWithoutFloorIdleTimeout()

            val client = createClient { install(WebSockets) }
            val testScope = CoroutineScope(Dispatchers.Default)
            val ownerHasFloor = CompletableDeferred<Unit>()
            val attackerDone = CompletableDeferred<Unit>()
            var participants: Set<String>? = null
            var attackerDenied = false

            val ownerToken = JwtConfig.generateToken("owner", "Owner", "device-owner")
            val attackerToken = JwtConfig.generateToken("attacker", "Attacker", "device-attacker")

            val owner =
                testScope.launch {
                    client.webSocket("/ws?token=$ownerToken") {
                        takeTheFloor(ownerHasFloor, attackerDone)
                    }
                }

            val attacker =
                testScope.launch {
                    ownerHasFloor.await()
                    client.webSocket("/ws?token=$attackerToken") {
                        val result = spoofTheOwner()
                        participants = result.first
                        attackerDenied = result.second
                        attackerDone.complete(Unit)
                        close()
                    }
                }

            owner.join()
            attacker.join()
            testScope.cancel()

            assertEquals(setOf("owner:Owner", "attacker:Attacker"), participants, "Join must not replace the owner")
            assertTrue(attackerDenied, "Spoofed StopSpeaking must not release the owner's floor")
        }

    /** Joins, takes the floor and holds it until the attacker is done. */
    private suspend fun DefaultClientWebSocketSession.takeTheFloor(
        hasFloor: CompletableDeferred<Unit>,
        until: CompletableDeferred<Unit>,
    ) {
        sendControl(ControlMessage.JoinChannel("room", "Owner", "owner"))
        sendControl(ControlMessage.StartSpeaking("room", "owner"))
        awaitMessage { it is ControlMessage.SpeakerChanged && it.userId == "owner" && it.isSpeaking }
        hasFloor.complete(Unit)
        until.await()
        close()
    }

    /** Sends messages that all claim to be the owner, and reports what the server made of them. */
    private suspend fun DefaultClientWebSocketSession.spoofTheOwner(): Pair<Set<String>?, Boolean> {
        sendControl(ControlMessage.JoinChannel("room", "Owner", "owner"))
        sendControl(ControlMessage.StopSpeaking("room", "owner"))
        sendControl(ControlMessage.StartSpeaking("room", "owner"))

        val list = awaitMessage { it is ControlMessage.ParticipantList && it.participants.size == 2 }
        val participants =
            (list as? ControlMessage.ParticipantList)
                ?.participants
                ?.map { "${it.userId}:${it.nickname}" }
                ?.toSet()
        val denied = awaitMessage { it is ControlMessage.FloorDenied } != null
        return participants to denied
    }

    private suspend fun DefaultClientWebSocketSession.sendControl(message: ControlMessage) {
        send(Frame.Text(Json.encodeToString<ControlMessage>(message)))
    }

    private suspend fun DefaultClientWebSocketSession.awaitMessage(predicate: (ControlMessage) -> Boolean) =
        withTimeoutOrNull(MESSAGE_TIMEOUT) {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val message = Json.decodeFromString<ControlMessage>(frame.readText())
                    if (predicate(message)) return@withTimeoutOrNull message
                }
            }
            null
        }

    private companion object {
        val MESSAGE_TIMEOUT = 10.seconds
    }
}
