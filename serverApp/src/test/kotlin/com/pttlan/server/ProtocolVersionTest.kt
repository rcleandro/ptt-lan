package com.pttlan.server

import com.pttlan.core.network.PROTOCOL_VERSION
import com.pttlan.server.auth.JwtConfig
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

/**
 * Phase 21.5: the handshake carries the protocol version so an incompatible client is told why, instead of
 * connecting and then failing on a message it cannot parse.
 */
class ProtocolVersionTest {
    private val token = JwtConfig.generateToken("u1", "Tester", "device-1")

    @Test
    fun refusesAnIncompatibleProtocolVersion() =
        testApplication {
            application { module() }
            val client = createClient { install(WebSockets) }

            client.webSocket("/ws?token=$token&protocol=${PROTOCOL_VERSION + 1}") {
                val reason =
                    withTimeoutOrNull(5.seconds) {
                        for (frame in incoming) { /* the server closes without sending anything */ }
                        closeReason.await()
                    }
                assertNotNull(reason)
                assertEquals("Versão do app incompatível com o servidor. Atualize o aplicativo.", reason.message)
            }
        }

    @Test
    fun acceptsTheCurrentProtocolAndClientsThatSendNone() =
        testApplication {
            application { module() }
            val client = createClient { install(WebSockets) }

            client.webSocket("/ws?token=$token&protocol=$PROTOCOL_VERSION") {
                assertNotNull(withTimeoutOrNull(5.seconds) { incoming.receive() }, "current protocol must connect")
            }

            val legacyToken = JwtConfig.generateToken("u2", "Legacy", "device-2")
            client.webSocket("/ws?token=$legacyToken") {
                assertNotNull(
                    withTimeoutOrNull(5.seconds) { incoming.receive() },
                    "a client that sends no protocol param must connect",
                )
            }
        }
}
