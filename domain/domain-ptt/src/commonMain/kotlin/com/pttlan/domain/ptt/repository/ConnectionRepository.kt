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

    fun discoverServers(): Flow<ServerNode>

    fun stopDiscovery()

    suspend fun connect(
        endpoint: ServerEndpoint,
        nickname: String,
    ): Result<Unit>

    fun disconnect()
}
