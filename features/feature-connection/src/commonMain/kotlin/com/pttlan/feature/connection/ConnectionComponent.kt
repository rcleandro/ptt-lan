package com.pttlan.feature.connection

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerNode
import com.russhwolf.settings.Settings
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
    val nickname: String = "",
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

    data class UpdateNickname(
        val nickname: String,
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
) : ComponentContext by componentContext, KoinComponent {
    private val settings: Settings by inject()

    private val _state = MutableStateFlow(
        ConnectionState(
            nickname = settings.getString("nickname", ""),
            manualIp = settings.getString("manualIp", "")
        )
    )
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
                if (_state.value.nickname.isBlank()) {
                    scope.launch { _effects.emit(ConnectionEffect.ShowError("Por favor, preencha o seu Nome")) }
                    return
                }
                settings.putString("nickname", _state.value.nickname)
                
                scope.launch {
                    val result = connectionRepository.connect(intent.server.host, intent.server.port, _state.value.nickname)
                    if (result.isFailure) {
                        _effects.emit(ConnectionEffect.ShowError("Falha ao conectar: ${result.exceptionOrNull()?.message}"))
                    }
                }
            }
            is ConnectionIntent.ConnectToManualIp -> {
                if (_state.value.nickname.isBlank()) {
                    scope.launch { _effects.emit(ConnectionEffect.ShowError("Por favor, preencha o seu Nome")) }
                    return
                }
                settings.putString("nickname", _state.value.nickname)
                settings.putString("manualIp", _state.value.manualIp)
                scope.launch {
                    val result = connectionRepository.connect(intent.ip, 9443, _state.value.nickname)
                    if (result.isFailure) {
                        _effects.emit(ConnectionEffect.ShowError("Falha ao conectar: ${result.exceptionOrNull()?.message}"))
                    }
                }
            }
            is ConnectionIntent.UpdateManualIp -> {
                _state.update { it.copy(manualIp = intent.ip.trim()) }
            }
            is ConnectionIntent.UpdateNickname -> {
                _state.update { it.copy(nickname = intent.nickname) }
            }
        }
    }
}
