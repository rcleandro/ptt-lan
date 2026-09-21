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
 * for every action on the WebSocket, so clients must use it as their local user id instead of generating one.
 */
@Serializable
data class LoginResponse(
    val token: String,
    val userId: String,
)
