package com.pttlan.feature.connection

import com.arkivanov.decompose.ComponentContext
import com.pttlan.core.common.network.isLocalNetwork
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.LocalServerHost
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode
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
    /** Room PIN: sent when joining, and set on the room when hosting. Blank means none. */
    val pin: String = "",
    /** Whether this platform can host the channel itself (host mode). */
    val canHost: Boolean = false,
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

    data class UpdatePin(
        val pin: String,
    ) : ConnectionIntent

    data object HostServer : ConnectionIntent
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
    private val localServerHost: LocalServerHost? = null,
) : ComponentContext by componentContext,
    KoinComponent {
    private val settings: Settings by inject()

    private val _state =
        MutableStateFlow(
            ConnectionState(
                nickname = settings.getString(SettingsKeys.NICKNAME, ""),
                manualIp = settings.getString(SettingsKeys.MANUAL_IP, ""),
                canHost = localServerHost != null,
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
                if (saveNickname()) connect(intent.server.endpoint, "Tempo de conexão excedido. O servidor está offline?")
            }

            is ConnectionIntent.ConnectToManualIp -> {
                if (saveNickname()) connectToManualIp(intent.ip)
            }

            is ConnectionIntent.HostServer -> {
                val host = localServerHost
                if (host != null && saveNickname()) hostServer(host)
            }

            is ConnectionIntent.UpdateManualIp -> {
                _state.update { it.copy(manualIp = intent.ip.trim()) }
            }

            is ConnectionIntent.UpdateNickname -> {
                _state.update { it.copy(nickname = intent.nickname) }
            }

            is ConnectionIntent.UpdatePin -> {
                _state.update { it.copy(pin = intent.pin.trim()) }
            }
        }
    }

    private fun connectToManualIp(ip: String) {
        settings.putString(SettingsKeys.MANUAL_IP, _state.value.manualIp)
        val endpoint =
            ServerEndpoint(
                host = ip,
                port = 9443,
                isLocal = isLocalNetwork(ip),
            )
        connect(endpoint, "Tempo de conexão excedido. Verifique o IP e tente novamente.")
    }

    private fun hostServer(host: LocalServerHost) {
        scope.launch {
            host
                .start(serviceName = "PTT-LAN-${_state.value.nickname}", pin = pinOrNull())
                .onSuccess { endpoint -> connect(endpoint, "Tempo de conexão excedido ao entrar no próprio canal.") }
                .onFailure { _effects.send(ConnectionEffect.ShowError("Não foi possível hospedar: ${it.message}")) }
        }
    }

    /**
     * Persists the nickname trimmed, or reports that it is missing and returns false. Trimmed here, not while
     * typing: it names the hosted room, and JmDNS never resolves a service whose name ends in a space.
     */
    private fun saveNickname(): Boolean {
        val nickname = _state.value.nickname.trim()
        if (nickname.isEmpty()) {
            scope.launch { _effects.send(ConnectionEffect.ShowError("Por favor, preencha o seu Nome")) }
            return false
        }
        _state.update { it.copy(nickname = nickname) }
        settings.putString(SettingsKeys.NICKNAME, nickname)
        return true
    }

    private fun pinOrNull(): String? = _state.value.pin.ifBlank { null }

    private fun connect(
        endpoint: ServerEndpoint,
        timeoutMessage: String,
    ) {
        scope.launch {
            val exception = connectToServerUseCase(endpoint, _state.value.nickname, pinOrNull()).exceptionOrNull() ?: return@launch
            if (exception is TimeoutCancellationException) {
                _effects.send(ConnectionEffect.ShowError(timeoutMessage))
            } else if (exception !is CancellationException) {
                _effects.send(ConnectionEffect.ShowError("Falha ao conectar: ${exception.message}"))
            }
        }
    }

    fun showError(message: String) {
        scope.launch { _effects.send(ConnectionEffect.ShowError(message)) }
    }
}
