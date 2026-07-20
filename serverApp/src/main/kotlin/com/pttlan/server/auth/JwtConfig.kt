package com.pttlan.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import kotlin.time.Duration.Companion.days

object JwtConfig {
    private const val ISSUER = "ptt-lan-server"
    private const val AUDIENCE = "ptt-lan-clients"

    // Gerar uma chave aleatória no boot para invalidar tokens de sessões anteriores
    private val SECRET =
        java.util.UUID
            .randomUUID()
            .toString()

    private val algorithm = Algorithm.HMAC256(SECRET)

    val verifier =
        JWT
            .require(algorithm)
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .build()

    fun generateToken(
        nickname: String,
        deviceId: String,
    ): String =
        JWT
            .create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("nickname", nickname)
            .withClaim("deviceId", deviceId)
            .withExpiresAt(Date(System.currentTimeMillis() + 1.days.inWholeMilliseconds))
            .sign(algorithm)
}
