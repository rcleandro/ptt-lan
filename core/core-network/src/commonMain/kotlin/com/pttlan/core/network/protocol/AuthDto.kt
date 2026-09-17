package com.pttlan.core.network.protocol

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val nickname: String,
    val deviceId: String,
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
