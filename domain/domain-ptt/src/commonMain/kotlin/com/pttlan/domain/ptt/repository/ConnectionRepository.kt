package com.pttlan.domain.ptt.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class ConnectionStatus {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting
}

data class ServerNode(val name: String, val host: String, val port: Int)

interface ConnectionRepository {
    val connectionStatus: StateFlow<ConnectionStatus>
    
    fun discoverServers(): Flow<ServerNode>
    fun stopDiscovery()
    
    suspend fun connect(host: String, port: Int = 9393): Result<Unit>
    fun disconnect()
}
