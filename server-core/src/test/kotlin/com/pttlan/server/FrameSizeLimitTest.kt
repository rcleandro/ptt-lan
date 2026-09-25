package com.pttlan.server

import com.pttlan.server.auth.JwtConfig
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

/**
 * 30.1: frames had no size limit, so anyone on the LAN with a token (any open room hands one out) could make the
 * server buffer one huge message; in host mode that server is the host's phone.
 */
class FrameSizeLimitTest {
    private val token = JwtConfig.generateToken("u1", "Tester", "device-1")

    @Test
    fun `a frame bigger than any real message closes the connection`() =
        testApplication {
            application { module() }
            val client = createClient { install(WebSockets) }

            client.webSocket("/ws?token=$token") {
                incoming.receive()
                outgoing.send(Frame.Binary(true, ByteArray(8 * 1024 * 1024)))

                val reason =
                    withTimeoutOrNull(5.seconds) {
                        for (frame in incoming) { /* drain until the server closes */ }
                        closeReason.await()
                    }
                assertNotNull(reason, "the server kept the connection after an 8 MB frame")
                assertEquals(CloseReason.Codes.TOO_BIG, reason.knownReason)
            }
        }

    @Test
    fun `audio sized frames still go through`() =
        testApplication {
            application { module() }
            val client = createClient { install(WebSockets) }

            client.webSocket("/ws?token=$token") {
                incoming.receive()
                // A generous live chunk: a second of 48 kHz 16 bit PCM would already be 96 KB, real chunks are 20 ms
                outgoing.send(Frame.Binary(true, ByteArray(16 * 1024)))

                val closed = withTimeoutOrNull(2.seconds) { closeReason.await() }
                assertEquals(null, closed, "a normal frame must not close the connection")
            }
        }
}
