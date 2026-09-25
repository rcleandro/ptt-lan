package com.pttlan.server.routing

import com.pttlan.core.network.protocol.LoginRequest
import com.pttlan.core.network.protocol.LoginResponse
import com.pttlan.server.auth.JwtConfig
import com.pttlan.server.auth.PinGuard
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

/** Room PIN from `ptt.roomPin`, set by the host mode (24.3). Null means an open room. */
fun Application.roomPin(): String? =
    environment.config
        .propertyOrNull("ptt.roomPin")
        ?.getString()
        ?.takeIf { it.isNotBlank() }

fun Route.authRoutes(roomPin: String? = null) {
    val pinGuard = roomPin?.let(::PinGuard)
    route("/api/auth") {
        rateLimit(RateLimitName("login")) {
            post("/login") {
                val request = runCatching { call.receive<LoginRequest>() }.getOrNull()
                if (request == null || request.nickname.isBlank() || request.deviceId.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request. 'nickname' and 'deviceId' are required.")
                    return@post
                }

                when (pinGuard?.check(request.pin)) {
                    PinGuard.Result.REJECTED -> {
                        call.respond(HttpStatusCode.Unauthorized, "PIN da sala incorreto")
                        return@post
                    }

                    PinGuard.Result.LOCKED -> {
                        call.respond(
                            HttpStatusCode.TooManyRequests,
                            "PIN errado muitas vezes seguidas: a sala ficou travada por alguns minutos",
                        )
                        return@post
                    }

                    PinGuard.Result.ACCEPTED, null -> {
                        Unit
                    }
                }

                val userId = UUID.randomUUID().toString()
                val token = JwtConfig.generateToken(userId, request.nickname, request.deviceId)
                call.respond(HttpStatusCode.OK, LoginResponse(token, userId))
            }
        }
    }
}
