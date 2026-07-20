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

fun isLocalNetwork(host: String): Boolean {
    val h = host.trim().lowercase()
    if (h == "localhost" || h == "127.0.0.1" || h.endsWith(".local")) return true
    val regex = Regex("""^(10\.\d{1,3}\.\d{1,3}\.\d{1,3})|(192\.168\.\d{1,3}\.\d{1,3})|(172\.(1[6-9]|2\d|3[0-1])\.\d{1,3}\.\d{1,3})$""")
    return regex.matches(h)
}

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
