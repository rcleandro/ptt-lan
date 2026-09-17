package com.pttlan.server

import com.pttlan.server.auth.JwtConfig
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.close
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * Phase 20.3: after a silent drop the old session stays open until the ping timeout (~20s). A client coming
 * back before that must not be refused its own nickname — the uniqueness rule applies between devices.
 */
class ReconnectSameDeviceTest {
    private fun token(
        userId: String,
        deviceId: String,
    ) = JwtConfig.generateToken(userId, "Tester", deviceId)

    @Test
    fun sameDeviceReconnectingReplacesItsOwnSession() =
        testApplication {
            application { module() }
            val client = createClient { install(WebSockets) }
            val scope = CoroutineScope(Dispatchers.Default)

            val staleSessionOpen = CompletableDeferred<Unit>()
            val staleSessionClosed = CompletableDeferred<String?>()

            val staleSession =
                scope.launch {
                    client.webSocket("/ws?token=${token("u1", "device-1")}") {
                        // The first frame is the channel broadcast the server sends once the session is registered,
                        // so waiting for it avoids racing with the handshake.
                        for (frame in incoming) {
                            staleSessionOpen.complete(Unit)
                        }
                        staleSessionClosed.complete(closeReason.await()?.message)
                    }
                }

            staleSessionOpen.await()

            // Same device, new login (the server issues a fresh userId on every login since 19.1)
            client.webSocket("/ws?token=${token("u2", "device-1")}") {
                assertEquals(
                    "Sessão substituída por uma nova conexão",
                    withTimeoutOrNull(5.seconds) { staleSessionClosed.await() },
                )
                close()
            }

            staleSession.cancel()
            scope.cancel()
        }

    @Test
    fun anotherDeviceCannotTakeTheSameNickname() =
        testApplication {
            application { module() }
            val client = createClient { install(WebSockets) }
            val scope = CoroutineScope(Dispatchers.Default)

            val firstSessionOpen = CompletableDeferred<Unit>()
            val firstSession =
                scope.launch {
                    client.webSocket("/ws?token=${token("u1", "device-1")}") {
                        for (frame in incoming) {
                            firstSessionOpen.complete(Unit)
                        }
                    }
                }

            firstSessionOpen.await()

            client.webSocket("/ws?token=${token("u2", "device-2")}") {
                // The close frame only lands once `incoming` is drained, so read until the server hangs up
                val reason =
                    withTimeoutOrNull(5.seconds) {
                        for (frame in incoming) { /* the server closes without sending anything */ }
                        closeReason.await()
                    }
                assertEquals("Nome já em uso", reason?.message)
            }

            firstSession.cancel()
            scope.cancel()
        }
}
