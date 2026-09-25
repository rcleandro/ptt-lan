package com.pttlan.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** [pins] keeps the certificates trusted on first use for LAN servers (30.5). */
expect fun createPlatformHttpClient(pins: CertificatePins): HttpClient

/** Ping traffic on an idle WebSocket. The socket timeout has to stay comfortably above it. */
internal const val WEBSOCKET_PING_INTERVAL_MS = 5_000L

/** Login and other plain requests: short, because the user is waiting for the screen to move on. */
private const val REQUEST_TIMEOUT_MS = 5_000L

/**
 * Six ping periods. It used to be 5s — the same as the ping interval — so a ping delayed by GC, a CPU spike
 * or a network handover was enough to tear down a healthy WebSocket on a silent channel.
 */
internal const val SOCKET_TIMEOUT_MS = 30_000L

fun createHttpClient(pins: CertificatePins = CertificatePins()): HttpClient =
    createPlatformHttpClient(pins).config {
        install(WebSockets) {
            pingIntervalMillis = WEBSOCKET_PING_INTERVAL_MS
        }

        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                },
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
            connectTimeoutMillis = REQUEST_TIMEOUT_MS
            socketTimeoutMillis = SOCKET_TIMEOUT_MS
        }

        install(Logging) {
            logger =
                object : Logger {
                    private val kermit =
                        co.touchlab.kermit.Logger
                            .withTag("network")

                    override fun log(message: String) {
                        kermit.d { message }
                    }
                }
            level = LogLevel.INFO
        }
    }
