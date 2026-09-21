package com.pttlan.server

import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.ServerRefusedException
import com.pttlan.core.network.createHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** Host mode (24.2): the embedded server answers over TLS on a real port, like `serverApp` does. */
class PttHostServerTest {
    private val port = ServerSocket(0).use { it.localPort }
    private val announced = mutableListOf<String>()
    private val server = PttHostServer(port) { _, name -> null.also { announced += name } }

    // Clients accept any certificate on the LAN; the test does the same with the in-memory one.
    private val trustAll =
        SSLContext.getInstance("TLS").apply {
            val manager =
                object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate>,
                        authType: String,
                    ) = Unit

                    override fun checkServerTrusted(
                        chain: Array<X509Certificate>,
                        authType: String,
                    ) = Unit

                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                }
            init(null, arrayOf<TrustManager>(manager), null)
        }

    @AfterTest
    fun tearDown() = server.stop()

    private fun post(
        path: String,
        body: String = """{"nickname":"host","deviceId":"d1"}""",
    ): HttpURLConnection =
        (URI("https://localhost:$port$path").toURL().openConnection() as HttpsURLConnection).apply {
            sslSocketFactory = trustAll.socketFactory
            hostnameVerifier = HostnameVerifier { _, _ -> true }
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(body.toByteArray()) }
        }

    private fun get(path: String): HttpURLConnection =
        (URI("https://localhost:$port$path").toURL().openConnection() as HttpsURLConnection).apply {
            sslSocketFactory = trustAll.socketFactory
            hostnameVerifier = HostnameVerifier { _, _ -> true }
        }

    @Test
    fun `serves login over tls and announces itself`() {
        server.start("PTT-LAN-host")

        val login = post("/api/auth/login")
        assertEquals(200, login.responseCode)
        assertTrue(
            login.inputStream
                .bufferedReader()
                .readText()
                .contains("token"),
        )
        assertEquals(listOf("PTT-LAN-host"), announced)
    }

    @Test
    fun `a room with a pin only lets in who knows it`() {
        server.start("PTT-LAN-host", pin = "4821")

        assertEquals(401, post("/api/auth/login").responseCode)
        assertEquals(200, post("/api/auth/login", """{"nickname":"host","deviceId":"d1","pin":"4821"}""").responseCode)
    }

    @Test
    fun `the app client sends the pin and reads a wrong one as a clear error`() =
        runBlocking {
            server.start("PTT-LAN-host", pin = "4821")
            val client = PttWebSocketClient(createHttpClient())

            val wrongPin =
                assertFailsWith<IllegalStateException> { client.login("localhost", port, true, "guest", "d2", "0000") }
            assertEquals("PIN da sala incorreto", wrongPin.message)
            assertTrue(client.login("localhost", port, true, "guest", "d2", "4821").token.isNotBlank())
        }

    @Test
    fun `ending the room tells the connected clients instead of leaving them retrying`() =
        runBlocking {
            // Stopping used to just drop the sockets: the others sat on "reconectando" for minutes.
            server.start("PTT-LAN-host")
            val client = PttWebSocketClient(createHttpClient())
            val token = client.login("localhost", port, true, "guest", "d3").token
            // The server's first message comes once it registered the session; `isConnected` is set before that.
            val registered = async(Dispatchers.Default) { client.controlMessages.first() }
            val session = async(Dispatchers.Default) { runCatching { client.connect("localhost", port, true, token) } }
            withTimeout(10.seconds) { registered.await() }

            server.stop()

            val ended = withTimeout(10.seconds) { session.await() }.exceptionOrNull()
            assertTrue(ended is ServerRefusedException, "got $ended")
            assertEquals("O host encerrou a sala", ended.message)
        }

    @Test
    fun `the admin panel is not served, so it cannot kill the hosting app nor touch JVM-only metrics`() {
        server.start("PTT-LAN-host")

        assertEquals(404, post("/api/admin/system/shutdown").responseCode)
        assertEquals(404, get("/api/admin/metrics").responseCode)
        assertEquals(404, get("/admin/index.html").responseCode)
    }

    @Test
    fun `start twice keeps one server and stop frees the port`() {
        server.start("PTT-LAN-host")
        server.start("PTT-LAN-host")
        assertEquals(1, announced.size)

        server.stop()

        assertFailsWith<ConnectException> { post("/api/auth/login").responseCode }
    }

    @Test
    fun `keeps the global koin of the app that hosts it`() {
        // The standard Ktor plugin stops whatever global Koin is running and starts its own in its place
        startKoin { modules(module { single { "app dependency" } }) }
        try {
            server.start("PTT-LAN-host")

            assertEquals(200, post("/api/auth/login").responseCode)
            assertEquals("app dependency", GlobalContext.get().get<String>())
        } finally {
            stopKoin()
        }
    }
}
