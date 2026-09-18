package com.pttlan.server

import com.pttlan.server.auth.JwtConfig
import com.pttlan.server.channel.ChannelRegistry
import com.pttlan.server.channel.DEFAULT_FLOOR_IDLE_TIMEOUT_MS
import com.pttlan.server.channel.DEFAULT_MAX_SPEECH_DURATION_MS
import com.pttlan.server.routing.adminPassword
import com.pttlan.server.routing.authRoutes
import com.pttlan.server.routing.dashboardRoutes
import com.pttlan.server.routing.pttRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.basic
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.KoinIsolated
import java.security.MessageDigest
import kotlin.time.Duration.Companion.seconds

private fun Application.longConfig(
    path: String,
    default: Long,
): Long =
    environment.config
        .propertyOrNull(path)
        ?.getString()
        ?.toLongOrNull() ?: default

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

    // Isolated: an app that embeds the server (host mode) already has its own global Koin.
    install(KoinIsolated) {
        modules(
            module {
                single {
                    ChannelRegistry(
                        floorIdleTimeoutMs = longConfig("ptt.floorIdleTimeoutMs", DEFAULT_FLOOR_IDLE_TIMEOUT_MS),
                        maxSpeechDurationMs = longConfig("ptt.maxSpeechDurationMs", DEFAULT_MAX_SPEECH_DURATION_MS),
                    )
                }
            },
        )
    }

    val adminPassword = adminPassword()

    install(Authentication) {
        basic("auth-admin") {
            realm = "PTT-LAN Admin"
            validate { credentials ->
                val password = adminPassword
                val matches =
                    password != null &&
                        credentials.name == "admin" &&
                        MessageDigest.isEqual(credentials.password.toByteArray(), password.toByteArray())
                if (matches) UserIdPrincipal(credentials.name) else null
            }
        }

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

    // Only behind a reverse proxy: otherwise any client could spoof its IP through X-Forwarded-For
    // and get its own rate limit bucket.
    if (environment.config
            .propertyOrNull("ptt.trustProxy")
            ?.getString()
            .toBoolean()
    ) {
        install(XForwardedHeaders)
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
        dashboardRoutes(adminEnabled = adminPassword != null)
    }
}
