package com.pttlan.feature.channellist

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.pttlan.core.common.MAX_CHANNEL_NAME_LENGTH
import com.pttlan.domain.ptt.repository.ActiveChannelDomain
import com.pttlan.domain.ptt.repository.ChannelDomain
import com.pttlan.domain.ptt.repository.LocalServerHost
import com.pttlan.domain.ptt.usecase.CreateChannelUseCase
import com.pttlan.domain.ptt.usecase.GetRecentChannelsUseCase
import com.pttlan.domain.ptt.usecase.JoinChannelUseCaseImpl
import com.pttlan.domain.ptt.usecase.ObserveActiveChannelsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChannelListState(
    val recentChannels: List<ChannelDomain> = emptyList(),
    val activeChannels: List<ActiveChannelDomain> = emptyList(),
    val newChannelName: String = "",
    /** Leaving while hosting ends the room for everyone, so it asks first. */
    val confirmingStopHost: Boolean = false,
)

sealed interface ChannelListIntent {
    data class UpdateNewChannelName(
        val name: String,
    ) : ChannelListIntent

    data class JoinChannel(
        val channelId: String,
        val name: String,
    ) : ChannelListIntent

    data object CreateChannel : ChannelListIntent

    /** Back arrow or system back: leaves the server, asking first when this device hosts the room. */
    data object Leave : ChannelListIntent

    data object ConfirmStopHost : ChannelListIntent

    data object DismissStopHost : ChannelListIntent
}

sealed interface ChannelListEffect {
    data class NavigateToChannel(
        val channelId: String,
    ) : ChannelListEffect

    /**
     * Disconnect and go back to the connection screen; [endRoom] also stops the room this device hosts. Done by
     * the root, which outlives this screen: leaving destroys it, and a stop() left in its scope never ran.
     */
    data class Leave(
        val endRoom: Boolean = false,
    ) : ChannelListEffect
}

class ChannelListComponent(
    componentContext: ComponentContext,
    private val getRecentChannelsUseCase: GetRecentChannelsUseCase,
    private val observeActiveChannelsUseCase: ObserveActiveChannelsUseCase,
    private val joinChannelUseCase: JoinChannelUseCaseImpl,
    private val createChannelUseCase: CreateChannelUseCase,
    private val localServerHost: LocalServerHost? = null,
) : ComponentContext by componentContext {
    private val _state = MutableStateFlow(ChannelListState())
    val state: StateFlow<ChannelListState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ChannelListEffect>()
    val effects: SharedFlow<ChannelListEffect> = _effects.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        // Without this the system back only popped the screen: the user stayed connected, and a host kept the
        // room running with no way back into it from the connection screen.
        backHandler.register(BackCallback { onIntent(ChannelListIntent.Leave) })
        // A new list is created for every session; without this each old one kept its collectors running.
        lifecycle.doOnDestroy { scope.cancel() }

        scope.launch {
            getRecentChannelsUseCase().collect { channels ->
                _state.update { it.copy(recentChannels = channels) }
            }
        }
        scope.launch {
            observeActiveChannelsUseCase().collect { active ->
                _state.update {
                    it.copy(
                        activeChannels =
                            active.sortedWith(
                                compareByDescending<ActiveChannelDomain> { ch -> ch.id == "Geral" }
                                    .thenBy { ch -> ch.id },
                            ),
                    )
                }
            }
        }
    }

    fun onIntent(intent: ChannelListIntent) {
        when (intent) {
            is ChannelListIntent.UpdateNewChannelName -> {
                _state.update { it.copy(newChannelName = intent.name.take(MAX_CHANNEL_NAME_LENGTH)) }
            }

            is ChannelListIntent.JoinChannel -> {
                scope.launch {
                    joinChannelUseCase(intent.channelId, intent.name)
                    _effects.emit(ChannelListEffect.NavigateToChannel(intent.channelId))
                }
            }

            is ChannelListIntent.Leave -> {
                if (localServerHost?.isHosting?.value == true) {
                    _state.update { it.copy(confirmingStopHost = true) }
                } else {
                    scope.launch { _effects.emit(ChannelListEffect.Leave()) }
                }
            }

            is ChannelListIntent.ConfirmStopHost -> {
                _state.update { it.copy(confirmingStopHost = false) }
                scope.launch { _effects.emit(ChannelListEffect.Leave(endRoom = true)) }
            }

            is ChannelListIntent.DismissStopHost -> {
                _state.update { it.copy(confirmingStopHost = false) }
            }

            is ChannelListIntent.CreateChannel -> {
                val name = _state.value.newChannelName
                if (name.isNotBlank()) {
                    // On a wide screen the list stays beside the channel just created, with the name still typed
                    _state.update { it.copy(newChannelName = "") }
                    scope.launch {
                        val id = createChannelUseCase(name)
                        _effects.emit(ChannelListEffect.NavigateToChannel(id))
                    }
                }
            }
        }
    }
}
