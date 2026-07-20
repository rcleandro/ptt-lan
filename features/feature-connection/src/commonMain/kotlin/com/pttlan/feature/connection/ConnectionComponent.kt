package com.pttlan.feature.connection

import com.arkivanov.decompose.ComponentContext
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode
import com.pttlan.domain.ptt.repository.isLocalNetwork
import com.pttlan.domain.ptt.usecase.ConnectToServerUseCase
import com.pttlan.domain.ptt.usecase.DiscoverServersUseCase
import com.pttlan.domain.ptt.usecase.ObserveConnectionStatusUseCase
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase,
    private val discoverServersUseCase: DiscoverServersUseCase,
    private val connectToServerUseCase: ConnectToServerUseCase,
) : ComponentContext by componentContext,
    KoinComponent {
    private val settings: Settings by inject()

    private val _state =
        MutableStateFlow(
            ConnectionState(
                nickname = settings.getString("nickname", ""),
                manualIp = settings.getString("manualIp", ""),
            ),
        )
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _effects = Channel<ConnectionEffect>(Channel.BUFFERED)
    val effects: Flow<ConnectionEffect> = _effects.receiveAsFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            observeConnectionStatusUseCase().collect { status ->
                _state.update { it.copy(status = status) }
                if (status == ConnectionStatus.Connected) {
                    _effects.send(ConnectionEffect.NavigateToChannelList)
                }
            }
        }

        scope.launch {
            discoverServersUseCase().collect { newServer ->
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
                    scope.launch { _effects.send(ConnectionEffect.ShowError("Por favor, preencha o seu Nome")) }
                    return
                }
                settings.putString("nickname", _state.value.nickname)

                scope.launch {
                    val result = connectToServerUseCase(intent.server.endpoint, _state.value.nickname)
                    if (result.isFailure) {
                        val exception = result.exceptionOrNull()
                        if (exception is TimeoutCancellationException) {
                            _effects.send(ConnectionEffect.ShowError("Tempo de conexão excedido. O servidor está offline?"))
                        } else if (exception !is CancellationException) {
                            _effects.send(ConnectionEffect.ShowError("Falha ao conectar: ${exception?.message}"))
                        }
                    }
                }
            }
            is ConnectionIntent.ConnectToManualIp -> {
                if (_state.value.nickname.isBlank()) {
                    scope.launch { _effects.send(ConnectionEffect.ShowError("Por favor, preencha o seu Nome")) }
                    return
                }
                settings.putString("nickname", _state.value.nickname)
                settings.putString("manualIp", _state.value.manualIp)
                scope.launch {
                    val endpoint =
                        ServerEndpoint(
                            host = intent.ip,
                            port = 9443,
                            isLocal = isLocalNetwork(intent.ip),
                        )
                    val result = connectToServerUseCase(endpoint, _state.value.nickname)
                    if (result.isFailure) {
                        val exception = result.exceptionOrNull()
                        if (exception is TimeoutCancellationException) {
                            _effects.send(ConnectionEffect.ShowError("Tempo de conexão excedido. Verifique o IP e tente novamente."))
                        } else if (exception !is CancellationException) {
                            _effects.send(ConnectionEffect.ShowError("Falha ao conectar: ${exception?.message}"))
                        }
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

    fun showError(message: String) {
        scope.launch { _effects.send(ConnectionEffect.ShowError(message)) }
    }
}
