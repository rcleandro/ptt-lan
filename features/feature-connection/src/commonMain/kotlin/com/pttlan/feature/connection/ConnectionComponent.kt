package com.pttlan.feature.connection

import com.arkivanov.decompose.ComponentContext
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConnectionState(
    val status: ConnectionStatus = ConnectionStatus.Disconnected,
    val discoveredServers: List<ServerNode> = emptyList(),
    val manualIp: String = "",
)

sealed interface ConnectionIntent {
    data class ConnectToDiscovered(
        val server: ServerNode,
    ) : ConnectionIntent

    data class ConnectToManualIp(
        val ip: String,
    ) : ConnectionIntent

    data class UpdateManualIp(
        val ip: String,
    ) : ConnectionIntent
}

sealed interface ConnectionEffect {
    data class ShowError(
        val message: String,
    ) : ConnectionEffect

    data object NavigateToChannelList : ConnectionEffect
}

class ConnectionComponent(
    componentContext: ComponentContext,
    private val connectionRepository: ConnectionRepository,
) : ComponentContext by componentContext {
    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ConnectionEffect>()
    val effects: SharedFlow<ConnectionEffect> = _effects.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            connectionRepository.connectionStatus.collect { status ->
                _state.update { it.copy(status = status) }
                if (status == ConnectionStatus.Connected) {
                    _effects.emit(ConnectionEffect.NavigateToChannelList)
                }
            }
        }

        scope.launch {
            connectionRepository.discoverServers().collect { newServer ->
                _state.update { currentState ->
                    val existing = currentState.discoveredServers
                    if (existing.any { it.name == newServer.name }) {
                        currentState
                    } else {
                        currentState.copy(discoveredServers = existing + newServer)
                    }
                }
            }
        }
    }

    fun onIntent(intent: ConnectionIntent) {
        when (intent) {
            is ConnectionIntent.ConnectToDiscovered -> {
                scope.launch {
                    val result = connectionRepository.connect(intent.server.host, intent.server.port)
                    if (result.isFailure) {
                        _effects.emit(ConnectionEffect.ShowError("Falha ao conectar: ${result.exceptionOrNull()?.message}"))
                    }
                }
            }
            is ConnectionIntent.ConnectToManualIp -> {
                scope.launch {
                    val result = connectionRepository.connect(intent.ip, 9443)
                    if (result.isFailure) {
                        _effects.emit(ConnectionEffect.ShowError("Falha ao conectar: ${result.exceptionOrNull()?.message}"))
                    }
                }
            }
            is ConnectionIntent.UpdateManualIp -> {
                _state.update { it.copy(manualIp = intent.ip.trim()) }
            }
        }
    }
}
