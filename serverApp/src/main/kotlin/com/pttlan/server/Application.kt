package com.pttlan.server

import com.pttlan.server.auth.JwtConfig
import com.pttlan.server.channel.ChannelRegistry
import com.pttlan.server.redis.RedisManager
import com.pttlan.server.routing.authRoutes
import com.pttlan.server.routing.dashboardRoutes
import com.pttlan.server.routing.pttRoutes
import io.ktor.network.tls.certificates.generateCertificate
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import kotlinx.io.IOException
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

private const val DEFAULT_PORT = 9443

fun main(args: Array<String>) {
    val keyStoreFile = File("build/keystore.jks")
    if (!keyStoreFile.exists()) {
        keyStoreFile.parentFile?.mkdirs()
        generateCertificate(keyStoreFile, keyAlias = "pttlan", keyPassword = "password", jksPassword = "password")
    }

    EngineMain.main(args)
}

@Suppress("LongMethod")
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

    startMdnsBroadcast(DEFAULT_PORT)

    install(Koin) {
        modules(
            module {
                single {
                    val redisManager = RedisManager()
                    redisManager.start()
                    redisManager
                }
                single {
                    ChannelRegistry(get())
                }
            },
        )
    }

    environment.monitor.subscribe(ApplicationStopped) {
        val koin =
            org.koin.java.KoinJavaComponent
                .getKoin()
        val redisManager: RedisManager = koin.get()
        redisManager.stop()
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "PTT-LAN Server"
            verifier(JwtConfig.verifier)
            validate { credential ->
                if (credential.payload.getClaim("nickname").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    install(RateLimit) {
        global {
            rateLimiter(limit = 100, refillPeriod = 60.seconds)
            requestKey { call -> call.request.origin.remoteHost }
        }
        register(
            RateLimitName("login"),
        ) {
            rateLimiter(limit = 5, refillPeriod = 60.seconds)
            requestKey { call -> call.request.origin.remoteHost }
        }
    }

    routing {
        authRoutes()
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
            println("Error starting JmDNS: ${e.message}")
        }
    }.start()
}
