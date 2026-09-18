package com.pttlan.data.ptt.repository

import co.touchlab.kermit.Logger
import com.pttlan.core.common.network.isLocalNetwork
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.discovery.ServerDiscoveryService
import com.pttlan.core.network.normalizeHost
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode
import com.russhwolf.settings.Settings
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Stable per-install identity. It must not derive from the nickname: that one changes and collides between
 * people with the same name, while the server uses `deviceId` to recognise a returning device (20.3).
 */
@OptIn(ExperimentalUuidApi::class)
internal fun deviceId(settings: Settings): String =
    settings.getStringOrNull(SettingsKeys.DEVICE_ID)
        ?: Uuid.random().toString().also { settings.putString(SettingsKeys.DEVICE_ID, it) }

class ConnectionRepositoryImpl(
    private val discoveryService: ServerDiscoveryService,
    private val webSocketClient: PttWebSocketClient,
    private val settings: Settings,
) : ConnectionRepository {
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.Disconnected)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    override var sessionUserId: String? = null
        private set

    override val lastDisconnectReason: String?
        get() = webSocketClient.lastCloseReason

    private val logger = Logger.withTag("network")
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
                    val login =
                        webSocketClient.login(
                            endpoint.host,
                            endpoint.port,
                            endpoint.isLocal,
                            nickname,
                            deviceId(settings),
                        )
                    sessionUserId = login.userId

                    // We launch the infinite reconnect loop in the background
                    webSocketClient.connect(endpoint.host, endpoint.port, endpoint.isLocal, login.token)
                } catch (e: Exception) {
                    deferred.completeExceptionally(e)
                    logger.w(e) { "Failed to connect to ${endpoint.host}:${endpoint.port}" }
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
        sessionUserId = null
        connectionJob?.cancel()
        monitorJob?.cancel()
        scope.launch {
            webSocketClient.disconnect()
        }
    }
}
