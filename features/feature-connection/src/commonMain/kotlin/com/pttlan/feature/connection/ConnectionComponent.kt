package com.pttlan.feature.connection

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.pttlan.core.common.DEFAULT_SERVER_PORT
import com.pttlan.core.common.HOSTED_ROOM_PREFIX
import com.pttlan.core.common.MIN_ROOM_PIN_LENGTH
import com.pttlan.core.common.RoomPinRejectedException
import com.pttlan.core.common.ServerCertificateChangedException
import com.pttlan.core.common.TooManyAttemptsException
import com.pttlan.core.common.network.isLocalNetwork
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.connection_disconnected
import com.pttlan.core.designsystem.generated.resources.connection_error_failed
import com.pttlan.core.designsystem.generated.resources.connection_error_host
import com.pttlan.core.designsystem.generated.resources.connection_error_nickname
import com.pttlan.core.designsystem.generated.resources.connection_error_pin_too_short
import com.pttlan.core.designsystem.generated.resources.connection_error_timeout_discovered
import com.pttlan.core.designsystem.generated.resources.connection_error_timeout_manual
import com.pttlan.core.designsystem.generated.resources.connection_error_timeout_own
import com.pttlan.core.designsystem.generated.resources.connection_error_too_many_attempts
import com.pttlan.core.designsystem.generated.resources.connection_error_wrong_pin
import com.pttlan.core.designsystem.generated.resources.connection_server_message
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.LocalServerHost
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode
import com.pttlan.domain.ptt.usecase.ConnectToServerUseCase
import com.pttlan.domain.ptt.usecase.DiscoverServersUseCase
import com.pttlan.domain.ptt.usecase.ObserveConnectionStatusUseCase
import com.pttlan.domain.ptt.usecase.TrustServerCertificateUseCase
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
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
    /** A LAN server showed another certificate than the trusted one: the user decides (30.5). */
    val certificateChange: CertificateChange? = null,
)

/** What the certificate dialog shows, and where to connect again if the user trusts the new certificate. */
data class CertificateChange(
    val endpoint: ServerEndpoint,
    val previousCode: String,
    val newCode: String,
    val timeoutMessage: StringResource,
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

    /** Starts the network search over, dropping hosts that already left. */
    data object RefreshServers : ConnectionIntent

    /** The user compared the codes and trusts the server's new certificate. */
    data object TrustNewCertificate : ConnectionIntent

    data object DismissCertificateChange : ConnectionIntent
}

sealed interface ConnectionEffect {
    /** [message] is a resource, resolved by the screen with [args]: no user text is built here. */
    data class ShowError(
        val message: StringResource,
        val args: List<Any> = emptyList(),
    ) : ConnectionEffect
}

class ConnectionComponent(
    componentContext: ComponentContext,
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase,
    private val discoverServersUseCase: DiscoverServersUseCase,
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val trustServerCertificateUseCase: TrustServerCertificateUseCase,
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

    /** Collected by the screen only. */
    val effects: Flow<ConnectionEffect> = _effects.receiveAsFlow()

    // Its own channel, collected by RootComponent only: a Channel hands each item to a single collector, and when
    // this shared [effects] with the screen, whichever collector came first took the navigation — on iOS the
    // screen, so the first tap connected but stayed on this screen.
    private val _navigateToChannelList = Channel<Unit>(Channel.CONFLATED)
    val navigateToChannelList: Flow<Unit> = _navigateToChannelList.receiveAsFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    private var discoveryJob: Job? = null

    init {
        // Without this the search outlived the screen: leaving the app with back while hosting keeps the process
        // alive, and the NSD search went on in the background, never stopped.
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            observeConnectionStatusUseCase().collect { status ->
                _state.update { it.copy(status = status) }
                if (status == ConnectionStatus.Connected) {
                    _navigateToChannelList.send(Unit)
                }
            }
        }

        startDiscovery()
    }

    private fun startDiscovery() {
        val previous = discoveryJob
        discoveryJob =
            scope.launch {
                // Each platform keeps its browser in a field, so the old search has to be torn down before the
                // new one starts, or its teardown would stop the new search.
                previous?.cancelAndJoin()
                _state.update { it.copy(discoveredServers = emptyList()) }
                discoverServersUseCase().collect { servers ->
                    _state.update { it.copy(discoveredServers = servers) }
                }
            }
    }

    fun onIntent(intent: ConnectionIntent) {
        when (intent) {
            is ConnectionIntent.ConnectToDiscovered -> {
                if (saveNickname()) connect(intent.server.endpoint, Res.string.connection_error_timeout_discovered)
            }

            is ConnectionIntent.ConnectToManualIp -> {
                if (saveNickname()) connectToManualIp(intent.ip)
            }

            is ConnectionIntent.HostServer -> {
                requestHosting()
            }

            is ConnectionIntent.RefreshServers -> {
                startDiscovery()
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

            is ConnectionIntent.TrustNewCertificate -> {
                trustNewCertificate()
            }

            is ConnectionIntent.DismissCertificateChange -> {
                _state.update { it.copy(certificateChange = null) }
            }
        }
    }

    private fun requestHosting() {
        val host = localServerHost
        val pin = _state.value.pin
        if (pin.isNotEmpty() && pin.length < MIN_ROOM_PIN_LENGTH) {
            // The host server refuses it too; checked here so the message is the app's, in its language
            scope.launch {
                _effects.send(ConnectionEffect.ShowError(Res.string.connection_error_pin_too_short, listOf(MIN_ROOM_PIN_LENGTH)))
            }
        } else if (host != null && saveNickname()) {
            hostServer(host)
        }
    }

    private fun trustNewCertificate() {
        val change = _state.value.certificateChange ?: return
        _state.update { it.copy(certificateChange = null) }
        // Explicit invoke: detekt's analysis misses the operator call here and flags the use case as unused
        trustServerCertificateUseCase.invoke(change.endpoint)
        connect(change.endpoint, change.timeoutMessage)
    }

    private fun connectToManualIp(ip: String) {
        settings.putString(SettingsKeys.MANUAL_IP, _state.value.manualIp)
        val endpoint =
            ServerEndpoint(
                host = ip,
                port = DEFAULT_SERVER_PORT,
                isLocal = isLocalNetwork(ip),
            )
        connect(endpoint, Res.string.connection_error_timeout_manual)
    }

    private fun hostServer(host: LocalServerHost) {
        scope.launch {
            host
                .start(serviceName = HOSTED_ROOM_PREFIX + _state.value.nickname, pin = pinOrNull())
                .onSuccess { endpoint -> connect(endpoint, Res.string.connection_error_timeout_own) }
                .onFailure { _effects.send(ConnectionEffect.ShowError(Res.string.connection_error_host, listOf(it.message.orEmpty()))) }
        }
    }

    /**
     * Persists the nickname trimmed, or reports that it is missing and returns false. Trimmed here, not while
     * typing: it names the hosted room, and JmDNS never resolves a service whose name ends in a space.
     */
    private fun saveNickname(): Boolean {
        val nickname = _state.value.nickname.trim()
        if (nickname.isEmpty()) {
            scope.launch { _effects.send(ConnectionEffect.ShowError(Res.string.connection_error_nickname)) }
            return false
        }
        _state.update { it.copy(nickname = nickname) }
        settings.putString(SettingsKeys.NICKNAME, nickname)
        return true
    }

    private fun pinOrNull(): String? = _state.value.pin.ifBlank { null }

    private fun connect(
        endpoint: ServerEndpoint,
        timeoutMessage: StringResource,
    ) {
        scope.launch {
            val exception = connectToServerUseCase(endpoint, _state.value.nickname, pinOrNull()).exceptionOrNull() ?: return@launch
            if (exception is ServerCertificateChangedException) {
                _state.update {
                    it.copy(certificateChange = CertificateChange(endpoint, exception.previousCode, exception.newCode, timeoutMessage))
                }
            } else if (exception is TimeoutCancellationException) {
                _effects.send(ConnectionEffect.ShowError(timeoutMessage))
            } else if (exception is RoomPinRejectedException) {
                _effects.send(ConnectionEffect.ShowError(Res.string.connection_error_wrong_pin))
            } else if (exception is TooManyAttemptsException) {
                _effects.send(ConnectionEffect.ShowError(Res.string.connection_error_too_many_attempts))
            } else if (exception !is CancellationException) {
                _effects.send(ConnectionEffect.ShowError(Res.string.connection_error_failed, listOf(exception.message.orEmpty())))
            }
        }
    }

    /** The session dropped: shows the reason the server gave, when there was one. */
    fun showDisconnected(serverReason: String?) {
        val error =
            if (serverReason == null) {
                ConnectionEffect.ShowError(Res.string.connection_disconnected)
            } else {
                ConnectionEffect.ShowError(Res.string.connection_server_message, listOf(serverReason))
            }
        scope.launch { _effects.send(error) }
    }
}
