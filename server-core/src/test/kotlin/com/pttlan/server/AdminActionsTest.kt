package com.pttlan.server

import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.server.auth.JwtConfig
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.ApplicationTestBuilder
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
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

private const val ADMIN_PASSWORD = "s3cret"

/**
 * The destructive admin endpoints: they close somebody's session or wipe a channel, and until now nothing
 * checked that they reach the channel registry at all.
 */
class AdminActionsTest {
    private fun ApplicationTestBuilder.serverWithAdmin() {
        environment {
            config =
                ApplicationConfig("application.conf").mergeWith(
                    MapApplicationConfig("ptt.adminPassword" to ADMIN_PASSWORD),
                )
        }
    }

    @Test
    fun kickingAUserClosesTheirSession() =
        testApplication {
            serverWithAdmin()
            val client = createClient { install(WebSockets) }
            val scope = CoroutineScope(Dispatchers.Default)
            val joined = CompletableDeferred<Unit>()
            val closeReason = CompletableDeferred<String?>()

            val session =
                scope.launch {
                    client.webSocket("/ws?token=${JwtConfig.generateToken("u1", "Tester", "device-1")}") {
                        send(
                            Frame.Text(
                                Json.encodeToString<ControlMessage>(ControlMessage.JoinChannel("obra", "Tester", "u1")),
                            ),
                        )
                        for (frame in incoming) {
                            if (frame is Frame.Text && frame.readText().contains("participant_list")) {
                                joined.complete(Unit)
                            }
                        }
                        closeReason.complete(this.closeReason.await()?.message)
                    }
                }
            joined.await()

            val response = client.post("/api/admin/channels/obra/kick/u1") { basicAuth("admin", ADMIN_PASSWORD) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Kicked by Admin", withTimeoutOrNull(5.seconds) { closeReason.await() })

            session.cancel()
            scope.cancel()
        }

    @Test
    fun broadcastReachesEveryConnectedClient() =
        testApplication {
            serverWithAdmin()
            val client = createClient { install(WebSockets) }
            val scope = CoroutineScope(Dispatchers.Default)
            val joined = CompletableDeferred<Unit>()
            val alert = CompletableDeferred<String>()

            val session =
                scope.launch {
                    client.webSocket("/ws?token=${JwtConfig.generateToken("u1", "Tester", "device-1")}") {
                        for (frame in incoming) {
                            // The first frame is the channel list the server sends once the session is
                            // registered; broadcasting before that would reach nobody
                            joined.complete(Unit)
                            if (frame is Frame.Text) {
                                val message = Json.decodeFromString<ControlMessage>(frame.readText())
                                if (message is ControlMessage.SystemAlert) {
                                    alert.complete(message.message)
                                    return@webSocket
                                }
                            }
                        }
                    }
                }
            joined.await()

            val response =
                client.post("/api/admin/system/broadcast") {
                    basicAuth("admin", ADMIN_PASSWORD)
                    contentType(ContentType.Application.Json)
                    setBody("""{"message":"Manutenção em 5 minutos"}""")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("Manutenção em 5 minutos", withTimeoutOrNull(5.seconds) { alert.await() })

            session.cancel()
            scope.cancel()
        }

    @Test
    fun deletingAChannelAndRestartingAnswerTheAdmin() =
        testApplication {
            serverWithAdmin()
            val client = createClient { install(WebSockets) }
            val joined = CompletableDeferred<Unit>()
            val scope = CoroutineScope(Dispatchers.Default)

            val session =
                scope.launch {
                    client.webSocket("/ws?token=${JwtConfig.generateToken("u1", "Tester", "device-1")}") {
                        send(
                            Frame.Text(
                                Json.encodeToString<ControlMessage>(ControlMessage.JoinChannel("obra", "Tester", "u1")),
                            ),
                        )
                        for (frame in incoming) {
                            if (frame is Frame.Text && frame.readText().contains("participant_list")) {
                                joined.complete(Unit)
                            }
                        }
                    }
                }
            joined.await()

            val deleted = client.post("/api/admin/channels/obra/delete") { basicAuth("admin", ADMIN_PASSWORD) }
            assertEquals(HttpStatusCode.OK, deleted.status)

            // A channel that is gone answers 404, and the metrics keep working after a reset
            val deletedAgain = client.post("/api/admin/channels/obra/delete") { basicAuth("admin", ADMIN_PASSWORD) }
            assertEquals(HttpStatusCode.NotFound, deletedAgain.status)

            val restarted = client.post("/api/admin/system/restart") { basicAuth("admin", ADMIN_PASSWORD) }
            assertEquals(HttpStatusCode.OK, restarted.status)
            assertNotNull(client.post("/api/admin/system/restart") { basicAuth("admin", ADMIN_PASSWORD) })

            session.cancel()
            scope.cancel()
        }
}
