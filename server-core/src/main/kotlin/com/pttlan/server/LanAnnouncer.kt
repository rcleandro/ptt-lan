package com.pttlan.server

import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

private fun getLocalIpAddress(): InetAddress? =
    NetworkInterface
        .getNetworkInterfaces()
        .toList()
        .asSequence()
        .filter { !it.isLoopback && it.isUp && !it.isVirtual && !it.isPointToPoint }
        .filter { isValidInterfaceName(it.name) }
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { it is Inet4Address }

internal fun isValidInterfaceName(name: String): Boolean {
    val lowerName = name.lowercase()
    val invalidPrefixes = listOf("docker", "utun", "tailscale", "awdl", "llw", "br-")
    return !invalidPrefixes.any { lowerName.startsWith(it) } &&
        !lowerName.contains("vbox") &&
        !lowerName.contains("vmnet")
}

/**
 * Announces the server as `_pttlan._tcp` so clients find it through mDNS. Blocks while JmDNS binds to the
 * interface; closing the returned handle withdraws the announcement. Null when the announcement failed —
 * the server still works, clients just have to type the address.
 */
fun announceOnLan(
    port: Int,
    serviceName: String,
): AutoCloseable? =
    try {
        val localIp = getLocalIpAddress()
        val jmdns = if (localIp != null) JmDNS.create(localIp) else JmDNS.create()
        jmdns.registerService(ServiceInfo.create("_pttlan._tcp.local.", serviceName, port, 0, 0, "SSL:$port"))
        AutoCloseable {
            jmdns.unregisterAllServices()
            jmdns.close()
        }
    } catch (e: IOException) {
        LoggerFactory.getLogger("com.pttlan.server.mdns").warn("Failed to start JmDNS: {}", e.message)
        null
    }
