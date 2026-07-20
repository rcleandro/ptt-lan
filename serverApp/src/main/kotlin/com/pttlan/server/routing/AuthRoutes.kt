package com.pttlan.server.routing

import com.pttlan.server.auth.JwtConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val nickname: String,
    val deviceId: String,
)

@Serializable
data class LoginResponse(
    val token: String,
)

fun Route.authRoutes() {
    route("/api/auth") {
        rateLimit(
            io.ktor.server.plugins.ratelimit
                .RateLimitName("login"),
        ) {
            post("/login") {
                val request = runCatching { call.receive<LoginRequest>() }.getOrNull()
                if (request == null || request.nickname.isBlank() || request.deviceId.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid request. 'nickname' and 'deviceId' are required.")
                    return@post
                }

                // Aqui entra a Opção 2/3. Para manter o offline-first em LAN simples, vamos apenas validar e emitir.
                // Para adicionar uma Senha de Sala global, adicionaríamos a checagem da senha aqui.

                val token = JwtConfig.generateToken(request.nickname, request.deviceId)
                call.respond(HttpStatusCode.OK, LoginResponse(token))
            }
        }
    }
}
