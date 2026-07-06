package com.pttlan.server

import com.pttlan.server.channel.ChannelRegistry
import com.pttlan.server.routing.pttRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.port
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import kotlinx.serialization.json.Json
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import kotlin.time.Duration.Companion.seconds

fun main() {
    embeddedServer(Netty, port = 9393, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

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
    val port = environment.config.port
    Thread {
        try {
            val jmdns = JmDNS.create(InetAddress.getLocalHost())
            val serviceInfo =
                ServiceInfo.create(
                    "_pttlan._tcp.local.",
                    "PTT-LAN-Server-${System.currentTimeMillis()}",
                    port,
                    "PTT LAN Server",
                )
            jmdns.registerService(serviceInfo)

            Runtime.getRuntime().addShutdownHook(
                Thread {
                    jmdns.unregisterAllServices()
                    jmdns.close()
                },
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }.start()

    install(org.koin.ktor.plugin.Koin) {
        modules(
            org.koin.dsl.module {
                single {
                    ChannelRegistry()
                }
            },
        )
    }

    routing {
        pttRoutes()
    }
}
