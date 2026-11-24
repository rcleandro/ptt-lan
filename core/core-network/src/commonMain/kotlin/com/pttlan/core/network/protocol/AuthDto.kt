package com.pttlan.core.network.protocol

import kotlinx.serialization.Serializable

/** [pin] is only checked by a server that hosts a room with a PIN (host mode); the others ignore it. */
@Serializable
data class LoginRequest(
    val nickname: String,
    val deviceId: String,
    val pin: String? = null,
)

/**
 * [userId] is issued by the server and is also the `sub` claim of [token]. It is the identity the server uses
 * for every action on the WebSocket, so clients must use it as their local user id instead of generating one. It is
 * the same for every login from one device, so a client that logs in again keeps its identity.
 */
@Serializable
data class LoginResponse(
    val token: String,
    val userId: String,
)

/**
 * Close reason for a token the server cannot verify, e.g. after a restart with a new signing key. A client that had
 * been connected logs in again instead of dropping the session.
 */
const val INVALID_TOKEN_REASON = "Token JWT inválido ou expirado"
