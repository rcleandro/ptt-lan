package com.pttlan.server

import io.ktor.network.tls.certificates.generateCertificate
import io.ktor.server.netty.EngineMain
import kotlinx.io.IOException
import org.slf4j.LoggerFactory
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

private const val DEFAULT_PORT = 9443

fun main(args: Array<String>) {
    val keyStoreFile = File("build/keystore.jks")
    if (!keyStoreFile.exists()) {
        // The same password must reach `ktor.security.ssl` in application.conf, hence the shared env var
        val keyStorePassword = System.getenv("PTT_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "password"
        keyStoreFile.parentFile?.mkdirs()
        generateCertificate(
            keyStoreFile,
            keyAlias = "pttlan",
            keyPassword = keyStorePassword,
            jksPassword = keyStorePassword,
        )
    }

    startMdnsBroadcast(DEFAULT_PORT)
    EngineMain.main(args)
}

private fun getLocalIpAddress(): InetAddress? =
    NetworkInterface
        .getNetworkInterfaces()
        .toList()
        .asSequence()
        .filter { !it.isLoopback && it.isUp && !it.isVirtual && !it.isPointToPoint }
        .filter { isValidInterfaceName(it.name) }
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { it is Inet4Address }

private fun isValidInterfaceName(name: String): Boolean {
    val lowerName = name.lowercase()
    val invalidPrefixes = listOf("docker", "utun", "tailscale", "awdl", "llw", "br-")
    return !invalidPrefixes.any { lowerName.startsWith(it) } &&
        !lowerName.contains("vbox") &&
        !lowerName.contains("vmnet")
}

private fun startMdnsBroadcast(port: Int) {
    Thread {
        try {
            val localIp = getLocalIpAddress()
            val jmdns = if (localIp != null) JmDNS.create(localIp) else JmDNS.create()
            val serviceInfo =
                ServiceInfo.create(
                    "_pttlan._tcp.local.",
                    "PTT-LAN-Server-${System.currentTimeMillis()}",
                    port,
                    0,
                    0,
                    "SSL:$port",
                )
            jmdns.registerService(serviceInfo)

            Runtime.getRuntime().addShutdownHook(
                Thread {
                    jmdns.unregisterAllServices()
                    jmdns.close()
                },
            )
        } catch (e: IOException) {
            LoggerFactory.getLogger("com.pttlan.server.mdns").warn("Failed to start JmDNS: {}", e.message)
        }
    }.start()
}
