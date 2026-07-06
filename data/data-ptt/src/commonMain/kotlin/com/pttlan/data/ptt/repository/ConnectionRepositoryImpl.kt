package com.pttlan.data.ptt.repository

import com.pttlan.core.network.discovery.ServerDiscoveryService
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerNode
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class ConnectionRepositoryImpl(
    private val httpClient: HttpClient,
    private val discoveryService: ServerDiscoveryService,
) : ConnectionRepository {
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    override fun discoverServers(): Flow<ServerNode> =
        discoveryService.discover().map {
            ServerNode(name = it.name, host = it.host, port = it.port)
        }

    override fun stopDiscovery() {
        discoveryService.stopDiscovery()
    }

    override suspend fun connect(
        host: String,
        port: Int,
    ): Result<Unit> {
        _connectionStatus.value = ConnectionStatus.Connecting
        // MVP: Fake connection for now until we fully implement the WebSocket handshake
        // We will just assume it succeeds for now
        _connectionStatus.value = ConnectionStatus.Connected
        return Result.success(Unit)
    }

    override fun disconnect() {
        _connectionStatus.value = ConnectionStatus.Disconnected
        // TODO: Close websockets and other connections
    }
}
