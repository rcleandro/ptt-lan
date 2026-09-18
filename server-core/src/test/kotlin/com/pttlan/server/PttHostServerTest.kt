package com.pttlan.server

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

    private fun post(path: String): HttpURLConnection =
        (URI("https://localhost:$port$path").toURL().openConnection() as HttpsURLConnection).apply {
            sslSocketFactory = trustAll.socketFactory
            hostnameVerifier = HostnameVerifier { _, _ -> true }
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write("""{"nickname":"host","deviceId":"d1"}""".toByteArray()) }
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
    fun `admin write routes stay off, so the panel cannot kill the hosting app`() {
        server.start("PTT-LAN-host")

        assertEquals(404, post("/api/admin/system/shutdown").responseCode)
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
