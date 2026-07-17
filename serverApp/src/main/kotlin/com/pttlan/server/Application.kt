package com.pttlan.server

import com.pttlan.server.channel.ChannelRegistry
import com.pttlan.server.routing.dashboardRoutes
import com.pttlan.server.routing.pttRoutes
import io.ktor.network.tls.certificates.generateCertificate
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import kotlin.time.Duration.Companion.seconds

@Suppress("TooGenericExceptionCaught", "MagicNumber")
fun main(args: Array<String>) {
    val keyStoreFile = File("build/keystore.jks")
    if (!keyStoreFile.exists()) {
        keyStoreFile.parentFile?.mkdirs()
        generateCertificate(keyStoreFile, keyAlias = "pttlan", keyPassword = "password", jksPassword = "password")
    }

    EngineMain.main(args)
}

@Suppress("TooGenericExceptionCaught", "MagicNumber")
fun Application.module() {
    install(WebSockets) {
        pingPeriod = 20.seconds
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
            },
        )
    }

    // Broadcast mDNS
    Thread {
        try {
            val localIp = getLocalIpAddress()
            val jmdns = if (localIp != null) JmDNS.create(localIp) else JmDNS.create()
            val serviceInfo =
                ServiceInfo.create(
                    "_pttlan._tcp.local.",
                    "PTT-LAN-Server-${System.currentTimeMillis()}",
                    9443,
                    0,
                    0,
                    "SSL:9443",
                )
            jmdns.registerService(serviceInfo)

            Runtime.getRuntime().addShutdownHook(
                Thread {
                    jmdns.unregisterAllServices()
                    jmdns.close()
                },
            )
        } catch (e: Exception) {
            println("Error starting JmDNS: ${e.message}")
        }
    }.start()

    install(Koin) {
        modules(
            module {
                single {
                    ChannelRegistry()
                }
            },
        )
    }

    routing {
        pttRoutes()
        dashboardRoutes()
    }
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
