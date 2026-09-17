package com.pttlan.core.network

import com.pttlan.core.network.protocol.ControlMessage
import io.ktor.network.tls.certificates.generateCertificate
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.io.File
import java.net.ServerSocket
import java.security.KeyStore
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private const val KEYSTORE_PASSWORD = "test-password"

/**
 * Phase 20.1: a drop must not end the session. The client keeps retrying with backoff, and once it is back
 * it re-sends the `JoinChannel` of the channel the user was in, so the UI can stay where it is.
 */
class PttWebSocketClientReconnectTest {
    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private fun testKeyStore(): KeyStore {
        val file = File.createTempFile("ptt-test-keystore", ".jks")
        file.delete()
        return generateCertificate(
            file = file,
            keyAlias = "pttlan",
            keyPassword = KEYSTORE_PASSWORD,
            jksPassword = KEYSTORE_PASSWORD,
        )
    }

    /** Boots a WSS endpoint that refuses every connection the way the server refuses a taken nickname. */
    private fun startRefusingServer(port: Int) =
        embeddedServer(
            factory = Netty,
            configure = {
                sslConnector(
                    keyStore = testKeyStore(),
                    keyAlias = "pttlan",
                    keyStorePassword = { KEYSTORE_PASSWORD.toCharArray() },
                    privateKeyPassword = { KEYSTORE_PASSWORD.toCharArray() },
                ) {
                    this.port = port
                }
            },
        ) {
            install(WebSockets)
            routing {
                webSocket("/ws") {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Nome já em uso"))
                }
            }
        }.start(wait = false)

    /** Boots a WSS endpoint that records the text frames it receives and closes the first session on demand. */
    private val reportedVersions = CopyOnWriteArrayList<String?>()

    private fun startServer(
        port: Int,
        received: Channel<String>,
        sessions: AtomicInteger,
        closeFirstSession: Boolean,
    ) = embeddedServer(
        factory = Netty,
        configure = {
            sslConnector(
                keyStore = testKeyStore(),
                keyAlias = "pttlan",
                keyStorePassword = { KEYSTORE_PASSWORD.toCharArray() },
                privateKeyPassword = { KEYSTORE_PASSWORD.toCharArray() },
            ) {
                this.port = port
            }
        },
    ) {
        install(WebSockets)
        routing {
            webSocket("/ws") {
                val session = sessions.incrementAndGet()
                reportedVersions.add(call.request.queryParameters["version"])
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        received.send(frame.readText())
                        if (closeFirstSession && session == 1) {
                            close()
                            return@webSocket
                        }
                    }
                }
            }
        }
    }.start(wait = false)

    private fun String.isJoinOf(channelId: String): Boolean =
        (Json.decodeFromString<ControlMessage>(this) as? ControlMessage.JoinChannel)?.channelId == channelId

    @Test
    fun reJoinsTheActiveChannelAfterReconnecting() =
        runBlocking {
            val port = freePort()
            val received = Channel<String>(capacity = Channel.UNLIMITED)
            val sessions = AtomicInteger()
            val server = startServer(port, received, sessions, closeFirstSession = true)
            val httpClient = createHttpClient()
            val client = PttWebSocketClient(httpClient, maxReconnectAttempts = 3)

            try {
                CoroutineScope(Dispatchers.Default).launch {
                    client.connect(host = "127.0.0.1", port = port, isLocal = true, token = "test-token")
                }

                withTimeout(10.seconds) { client.isConnected.first { it } }
                client.sendControlMessage(ControlMessage.JoinChannel("Geral", "Tester", "u1"))

                // First join, sent by the caller; the server closes the session right after it
                val firstJoin = withTimeoutOrNull(10.seconds) { received.receive() }
                assertNotNull(firstJoin)
                assertTrue(firstJoin.isJoinOf("Geral"))

                // Second join, replayed by the client itself once it reconnects
                val reJoin = withTimeoutOrNull(20.seconds) { received.receive() }
                assertNotNull(reJoin, "the client should re-send JoinChannel after reconnecting")
                assertTrue(reJoin.isJoinOf("Geral"))
                assertEquals(2, sessions.get())
                // 20.5: the panel shows "Desconhecida" unless the client sends its version on every handshake
                assertEquals(listOf(APP_VERSION, APP_VERSION), reportedVersions.toList())
            } finally {
                client.disconnect()
                httpClient.close()
                server.stop(0, 0)
            }
        }

    @Test
    fun givesUpAfterTheConfiguredNumberOfAttempts() =
        runBlocking {
            val port = freePort()
            val received = Channel<String>(capacity = Channel.UNLIMITED)
            val server = startServer(port, received, AtomicInteger(), closeFirstSession = false)
            val httpClient = createHttpClient()
            val client = PttWebSocketClient(httpClient, maxReconnectAttempts = 2)

            try {
                val connectLoop =
                    CoroutineScope(Dispatchers.Default).launch {
                        client.connect(host = "127.0.0.1", port = port, isLocal = true, token = "test-token")
                    }

                withTimeout(10.seconds) { client.isConnected.first { it } }
                server.stop(0, 0)

                // Without the attempt limit this loop would retry forever and the UI would never be told
                withTimeout(30.seconds) { connectLoop.join() }
                assertEquals(false, client.isConnected.value)
            } finally {
                client.disconnect()
                httpClient.close()
            }
        }

    @Test
    fun surfacesTheReasonWhenTheServerRefusesTheConnection() =
        runBlocking {
            val port = freePort()
            val server = startRefusingServer(port)
            val httpClient = createHttpClient()
            val client = PttWebSocketClient(httpClient)

            try {
                val error =
                    assertFailsWith<ServerRefusedException> {
                        withTimeout(15.seconds) {
                            client.connect(host = "127.0.0.1", port = port, isLocal = true, token = "test-token")
                        }
                    }

                assertEquals("Nome já em uso", error.message)
                assertEquals("Nome já em uso", client.lastCloseReason)
            } finally {
                httpClient.close()
                server.stop(0, 0)
            }
        }
}
