package com.pttlan.core.network

import com.pttlan.core.common.ServerTimeoutException
import io.ktor.http.ContentType
import io.ktor.network.tls.certificates.generateCertificate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.sslConnector
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

private const val PASSWORD = "test-password"
private const val LOGIN_JSON = """{"token":"t","userId":"u"}"""

/**
 * Achado 6 da rodada da 29.1: the first login of a just-opened app timed out and the user read Ktor's
 * "Request timeout has expired [url=...]". A timed-out login is tried once more, and only then reported as the
 * server not answering.
 */
class LoginRetryTest {
    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    /** A login endpoint that answers after the timeout for the first [slowAnswers] requests. */
    private fun startServer(
        port: Int,
        slowAnswers: Int,
        requests: AtomicInteger,
    ) = embeddedServer(
        factory = Netty,
        configure = {
            val file = File.createTempFile("ptt-login-keystore", ".jks").also { it.delete() }
            sslConnector(
                keyStore = generateCertificate(file, keyAlias = "pttlan", keyPassword = PASSWORD, jksPassword = PASSWORD),
                keyAlias = "pttlan",
                keyStorePassword = { PASSWORD.toCharArray() },
                privateKeyPassword = { PASSWORD.toCharArray() },
            ) {
                this.port = port
            }
        },
    ) {
        routing {
            post("/api/auth/login") {
                if (requests.incrementAndGet() <= slowAnswers) delay((REQUEST_TIMEOUT_MS + 1_000).milliseconds)
                call.respondText(LOGIN_JSON, ContentType.Application.Json)
            }
        }
    }.start(wait = false)

    @Test
    fun aLoginThatTimesOutOnceIsTriedAgain() =
        runBlocking {
            val port = freePort()
            val requests = AtomicInteger()
            val server = startServer(port, slowAnswers = 1, requests)
            val httpClient = createHttpClient()
            try {
                val login = PttWebSocketClient(httpClient).login("127.0.0.1", port, isLocal = true, "Tester", "device-1")
                assertEquals("u", login.userId)
                assertEquals(2, requests.get())
            } finally {
                httpClient.close()
                server.stop(0, 0)
            }
        }

    @Test
    fun aServerThatNeverAnswersInTimeIsReportedAsATimeout() =
        runBlocking {
            val port = freePort()
            val requests = AtomicInteger()
            val server = startServer(port, slowAnswers = Int.MAX_VALUE, requests)
            val httpClient = createHttpClient()
            try {
                assertFailsWith<ServerTimeoutException> {
                    PttWebSocketClient(httpClient).login("127.0.0.1", port, isLocal = true, "Tester", "device-1")
                }
                assertEquals(2, requests.get())
            } finally {
                httpClient.close()
                server.stop(0, 0)
            }
        }
}
