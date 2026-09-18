package com.pttlan.android

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.createHttpClient
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.server.PttHostServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Spike 24.4 (ADR 0010): can an Android device run the server? Covers what one device can prove on its own —
 * Netty + TLS with the in-memory certificate, login with room PIN, a WebSocket session and the NSD
 * announcement. The multi-device criterion (two phones talking for 10 min with the host's screen locked)
 * needs real hardware and is checked by hand.
 */
@RunWith(AndroidJUnit4::class)
class HostModeSpikeTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val server = PttHostServer(announce = { port, name -> announceWithNsd(context, port, name) })
    private val host = AndroidServerHost(context)

    @After
    fun tearDown() {
        server.stop()
        host.stop()
    }

    @Test
    fun serverRunsOnTheDeviceAndTheAppClientTalksToIt() =
        runBlocking {
            val endpoint = host.start("PTT-LAN-spike", pin = "4821").getOrThrow()
            assertTrue(host.isHosting.value)
            val client = PttWebSocketClient(createHttpClient())

            val login = client.login(endpoint.host, endpoint.port, true, "spike", "device-spike", "4821")
            assertTrue(login.token.isNotBlank())

            launch(Dispatchers.IO) { client.connect("localhost", 9443, true, login.token) }
            withTimeout(10.seconds) { client.isConnected.first { it } }
            val participantList =
                async { client.controlMessages.first { it is ControlMessage.ParticipantList } as ControlMessage.ParticipantList }
            client.sendControlMessage(ControlMessage.JoinChannel("Geral", "spike", login.userId))
            val participants = withTimeout(10.seconds) { participantList.await() }
            assertEquals(listOf("spike"), participants.participants.map { it.nickname })
            client.disconnect()
        }

    @Test
    fun serverIsFoundThroughNsd() {
        server.start("PTT-LAN-spike-nsd")

        val nsd = context.getSystemService(NsdManager::class.java)
        val found = CountDownLatch(1)
        var foundName: String? = null
        val listener =
            object : NsdManager.DiscoveryListener {
                override fun onServiceFound(info: NsdServiceInfo) {
                    if (info.serviceName == "PTT-LAN-spike-nsd") {
                        foundName = info.serviceName
                        found.countDown()
                    }
                }

                override fun onDiscoveryStarted(serviceType: String) = Unit

                override fun onDiscoveryStopped(serviceType: String) = Unit

                override fun onServiceLost(info: NsdServiceInfo) = Unit

                override fun onStartDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int,
                ) = Unit

                override fun onStopDiscoveryFailed(
                    serviceType: String,
                    errorCode: Int,
                ) = Unit
            }
        nsd.discoverServices("_pttlan._tcp", NsdManager.PROTOCOL_DNS_SD, listener)
        try {
            found.await(15, TimeUnit.SECONDS)
            assertNotNull("NSD did not find the announced server", foundName)
        } finally {
            nsd.stopServiceDiscovery(listener)
        }
    }
}
