package com.pttlan.data.ptt.repository

import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.discovery.ServerDiscoveryService
import com.pttlan.core.network.normalizeHost
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode
import com.pttlan.domain.ptt.repository.isLocalNetwork
import kotlinx.coroutines.CompletableDeferred
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
    private val discoveryService: ServerDiscoveryService,
    private val webSocketClient: PttWebSocketClient,
) : ConnectionRepository {
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default)
    private var connectionJob: Job? = null
    private var monitorJob: Job? = null

    override fun discoverServers(): Flow<ServerNode> =
        discoveryService.discover().map {
            val normalizedHost = normalizeHost(it.host)
            ServerNode(
                name = it.name,
                endpoint =
                    ServerEndpoint(
                        host = normalizedHost,
                        port = it.port,
                        isLocal = isLocalNetwork(normalizedHost),
                    ),
            )
        }

    override fun stopDiscovery() {
        discoveryService.stopDiscovery()
    }

    override suspend fun connect(
        endpoint: ServerEndpoint,
        nickname: String,
    ): Result<Unit> {
        _connectionStatus.value = ConnectionStatus.Connecting

        connectionJob?.cancel()
        monitorJob?.cancel()
        val deferred = CompletableDeferred<Unit>()

        connectionJob =
            scope.launch {
                try {
                    // We launch the infinite reconnect loop in the background
                    webSocketClient.connect(endpoint.host, endpoint.port, endpoint.isLocal, nickname)
                } catch (e: Exception) {
                    deferred.completeExceptionally(e)
                    e.printStackTrace()
                } finally {
                    _connectionStatus.value = ConnectionStatus.Disconnected
                }
            }

        monitorJob =
            scope.launch {
                webSocketClient.isConnected.collect { isConnected ->
                    if (isConnected) {
                        _connectionStatus.value = ConnectionStatus.Connected
                        deferred.complete(Unit)
                    } else if (_connectionStatus.value == ConnectionStatus.Connected) {
                        _connectionStatus.value = ConnectionStatus.Reconnecting
                    }
                }
            }

        return try {
            deferred.await()
            Result.success(Unit)
        } catch (e: Exception) {
            monitorJob?.cancel()
            Result.failure(e)
        }
    }

    override fun disconnect() {
        _connectionStatus.value = ConnectionStatus.Disconnected
        connectionJob?.cancel()
        monitorJob?.cancel()
        scope.launch {
            webSocketClient.disconnect()
        }
    }
}
