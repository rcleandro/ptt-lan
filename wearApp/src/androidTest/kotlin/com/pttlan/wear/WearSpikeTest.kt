package com.pttlan.wear

import android.Manifest
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pttlan.core.audio.OpusAudioCodec
import com.pttlan.core.audio.createAudioRecorder
import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.createHttpClient
import com.pttlan.core.network.protocol.ControlMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

private const val TAG = "WearSpike"

/**
 * Spike 25.1 (ADR 0011): can the watch be a client on its own? Covers what a test on the watch can prove —
 * Wi-Fi brought up on demand, NSD over it, joining a channel with the app's client, Opus capture and the audio
 * outputs. Needs a server or host on the LAN; the 30-minute conversation and the battery are checked by hand.
 *
 * Run with the watch on adb: `./gradlew :wearApp:connectedDebugAndroidTest`, adding
 * `-Pandroid.testInstrumentationRunnerArguments.pin=<PIN>` for a room with a PIN.
 */
@RunWith(AndroidJUnit4::class)
class WearSpikeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private var wifiCallback: ConnectivityManager.NetworkCallback? = null

    @Before
    fun bringUpWifi() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, "android.permission.ACCESS_LOCAL_NETWORK")
        // In real use the app is open on the watch: with no screen of its own the system may cut its network
        context.startActivity(
            android.content.Intent(context, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        instrumentation.waitForIdleSync()

        // Wear OS uses the phone's connection over Bluetooth and keeps Wi-Fi off; the LAN only answers on Wi-Fi
        val available = CountDownLatch(1)
        var wifi: Network? = null
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    wifi = network
                    available.countDown()
                }

                override fun onBlockedStatusChanged(
                    network: Network,
                    blocked: Boolean,
                ) {
                    Log.i(TAG, "Wi-Fi blocked for this app: $blocked")
                }
            }
        val startedAt = System.currentTimeMillis()
        connectivity.requestNetwork(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
            callback,
        )
        wifiCallback = callback
        assertTrue("Wi-Fi did not come up in 30 s", available.await(30, TimeUnit.SECONDS))
        Log.i(TAG, "Wi-Fi up in ${System.currentTimeMillis() - startedAt} ms")
        assertTrue("could not bind to Wi-Fi", connectivity.bindProcessToNetwork(wifi))
    }

    @After
    fun releaseWifi() {
        connectivity.bindProcessToNetwork(null)
        wifiCallback?.let(connectivity::unregisterNetworkCallback)
    }

    @Test
    fun findsAServerAndJoinsAChannelOverWifi() =
        runBlocking {
            val startedAt = System.currentTimeMillis()
            val server = discoverServer()
            Log.i(TAG, "Found ${server.serviceName} at ${server.host?.hostAddress}:${server.port} " +
                "in ${System.currentTimeMillis() - startedAt} ms")
            val host = checkNotNull(server.host?.hostAddress) { "the server resolved without an address" }
            Log.i(TAG, "Wi-Fi caps: ${connectivity.getNetworkCapabilities(connectivity.boundNetworkForProcess)}")
            val tcp =
                runCatching { java.net.Socket().use { it.connect(java.net.InetSocketAddress(host, server.port), 3_000) } }
            Log.i(TAG, "Plain TCP to $host:${server.port}: ${tcp.exceptionOrNull() ?: "ok"}")

            val client = PttWebSocketClient(createHttpClient())
            val pin = InstrumentationRegistry.getArguments().getString("pin")
            val login = client.login(host, server.port, true, "watch-spike", "watch-spike-device", pin)
            launch(Dispatchers.IO) { client.connect(host, server.port, true, login.token) }
            withTimeout(15.seconds) { client.isConnected.first { it } }

            val participants =
                async { client.controlMessages.first { it is ControlMessage.ParticipantList } as ControlMessage.ParticipantList }
            client.sendControlMessage(ControlMessage.JoinChannel("Geral", "watch-spike", login.userId))
            val names = withTimeout(10.seconds) { participants.await() }.participants.map { it.nickname }
            Log.i(TAG, "Joined Geral with $names")
            assertTrue(names.contains("watch-spike"))
            client.disconnect()
        }

    @Test
    fun capturesAndEncodesWithOpus() =
        runBlocking {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
            val recorder = createAudioRecorder()
            val codec = OpusAudioCodec()

            // 25 frames of 20 ms: half a second from the watch's microphone
            val frames = withTimeout(5.seconds) { recorder.startCapture().take(25).toList() }
            recorder.stopCapture()
            val encoded = frames.map(codec::encode)

            Log.i(TAG, "Captured ${frames.size} frames of ${frames.first().size} bytes; Opus ${encoded.map { it.size }}")
            assertTrue("Opus rejected the watch's frames", encoded.all { it.isNotEmpty() })
        }

    @Test
    fun hasSomewhereToPlayAudio() {
        val outputs = context.getSystemService(AudioManager::class.java).getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val types = outputs.map { it.type }
        Log.i(TAG, "Audio outputs: ${outputs.map { "${it.productName} (type ${it.type})" }}")
        assertTrue(
            "no speaker nor Bluetooth headset",
            types.any { it == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER || it == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP },
        )
    }

    private fun discoverServer(): NsdServiceInfo {
        val nsd = context.getSystemService(NsdManager::class.java)
        val resolved = CountDownLatch(1)
        var server: NsdServiceInfo? = null
        val listener =
            object : NsdManager.DiscoveryListener {
                @Suppress("DEPRECATION")
                override fun onServiceFound(info: NsdServiceInfo) {
                    nsd.resolveService(
                        info,
                        object : NsdManager.ResolveListener {
                            override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                                server = resolvedInfo
                                resolved.countDown()
                            }

                            override fun onResolveFailed(
                                failed: NsdServiceInfo,
                                errorCode: Int,
                            ) = Unit
                        },
                    )
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
            resolved.await(30, TimeUnit.SECONDS)
            return checkNotNull(server) { "NSD found no server over the watch's Wi-Fi" }
        } finally {
            nsd.stopServiceDiscovery(listener)
        }
    }
}
