package com.pttlan.domain.ptt.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class ConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
}

data class ServerEndpoint(
    val host: String,
    val port: Int,
    val isLocal: Boolean,
)

data class ServerNode(
    val name: String,
    val endpoint: ServerEndpoint,
)

interface ConnectionRepository {
    val connectionStatus: StateFlow<ConnectionStatus>

    /**
     * Identity issued by the server at login (JWT `sub`). The server ignores ids sent in messages and uses
     * this one, so it is the only id that matches `SpeakerChanged`/`ParticipantList` for the local user.
     * Null while not logged in.
     */
    val sessionUserId: String?

    fun discoverServers(): Flow<ServerNode>

    fun stopDiscovery()

    suspend fun connect(
        endpoint: ServerEndpoint,
        nickname: String,
    ): Result<Unit>

    fun disconnect()
}
