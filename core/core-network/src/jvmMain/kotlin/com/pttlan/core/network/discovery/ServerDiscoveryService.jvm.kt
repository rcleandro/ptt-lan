package com.pttlan.core.network.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener

actual class ServerDiscoveryService actual constructor() {
    private var jmdns: JmDNS? = null

    actual fun discover(): Flow<DiscoveredServer> =
        callbackFlow {
            val serviceType = "_pttlan._tcp.local."
            try {
                jmdns =
                    withContext(Dispatchers.IO) {
                        val localIp = getLocalIpAddress()
                        if (localIp != null) JmDNS.create(localIp) else JmDNS.create()
                    }

                val listener =
                    object : ServiceListener {
                        override fun serviceAdded(event: ServiceEvent) {
                            jmdns?.requestServiceInfo(event.type, event.name)
                        }

                        override fun serviceRemoved(event: ServiceEvent) {
                            // Could handle removal if needed
                        }

                        override fun serviceResolved(event: ServiceEvent) {
                            val info = event.info
                            val host = info.hostAddresses.firstOrNull() ?: return
                            val server =
                                DiscoveredServer(
                                    name = info.name,
                                    host = host,
                                    port = info.port,
                                )
                            trySend(server)
                        }
                    }

                jmdns?.addServiceListener(serviceType, listener)

                awaitClose {
                    jmdns?.removeServiceListener(serviceType, listener)
                }
            } catch (e: Exception) {
                close(e)
            }
        }

    actual fun stopDiscovery() {
        jmdns?.close()
        jmdns = null
    }

    private fun getLocalIpAddress(): InetAddress? =
        java.net.NetworkInterface
            .getNetworkInterfaces()
            .toList()
            .asSequence()
            .filter { !it.isLoopback && it.isUp && !it.isVirtual && !it.isPointToPoint }
            .filter { isValidInterfaceName(it.name) }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is java.net.Inet4Address }

    private fun isValidInterfaceName(name: String): Boolean {
        val lowerName = name.lowercase()
        val invalidPrefixes = listOf("docker", "utun", "tailscale", "awdl", "llw", "br-")
        return !invalidPrefixes.any { lowerName.startsWith(it) } &&
            !lowerName.contains("vbox") &&
            !lowerName.contains("vmnet")
    }
}
