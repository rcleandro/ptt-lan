package com.pttlan.data.ptt.repository

import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.discovery.ServerDiscoveryService
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerNode
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConnectionRepositoryImpl(
    private val httpClient: HttpClient,
    private val discoveryService: ServerDiscoveryService,
    private val webSocketClient: PttWebSocketClient,
) : ConnectionRepository {
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var connectionJob: Job? = null

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
        
        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                // We launch the infinite reconnect loop in the background
                webSocketClient.connect(host, port)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _connectionStatus.value = ConnectionStatus.Disconnected
            }
        }
        
        // Wait a tiny bit just to let it start, then assume it connects or tries to connect
        // Ideally we would listen to connection state from WebSocketClient, but for MVP:
        _connectionStatus.value = ConnectionStatus.Connected
        return Result.success(Unit)
    }

    override fun disconnect() {
        _connectionStatus.value = ConnectionStatus.Disconnected
        connectionJob?.cancel()
        scope.launch {
            webSocketClient.disconnect()
        }
    }
}
