package com.pttlan.feature.channellist

import com.arkivanov.decompose.ComponentContext
import com.pttlan.domain.ptt.repository.ActiveChannelDomain
import com.pttlan.domain.ptt.repository.ChannelDomain
import com.pttlan.domain.ptt.usecase.CreateChannelUseCase
import com.pttlan.domain.ptt.usecase.GetRecentChannelsUseCase
import com.pttlan.domain.ptt.usecase.JoinChannelUseCaseImpl
import com.pttlan.domain.ptt.usecase.ObserveActiveChannelsUseCase
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

data class ChannelListState(
    val recentChannels: List<ChannelDomain> = emptyList(),
    val activeChannels: List<ActiveChannelDomain> = emptyList(),
    val newChannelName: String = "",
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
}

sealed interface ChannelListEffect {
    data class NavigateToChannel(
        val channelId: String,
    ) : ChannelListEffect
}

class ChannelListComponent(
    componentContext: ComponentContext,
    private val getRecentChannelsUseCase: GetRecentChannelsUseCase,
    private val observeActiveChannelsUseCase: ObserveActiveChannelsUseCase,
    private val joinChannelUseCase: JoinChannelUseCaseImpl,
    private val createChannelUseCase: CreateChannelUseCase,
) : ComponentContext by componentContext {
    private val _state = MutableStateFlow(ChannelListState())
    val state: StateFlow<ChannelListState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ChannelListEffect>()
    val effects: SharedFlow<ChannelListEffect> = _effects.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            getRecentChannelsUseCase().collect { channels ->
                _state.update { it.copy(recentChannels = channels) }
            }
        }
        scope.launch {
            observeActiveChannelsUseCase().collect { active ->
                _state.update { it.copy(activeChannels = active) }
            }
        }
    }

    fun onIntent(intent: ChannelListIntent) {
        when (intent) {
            is ChannelListIntent.UpdateNewChannelName -> {
                _state.update { it.copy(newChannelName = intent.name) }
            }
            is ChannelListIntent.JoinChannel -> {
                scope.launch {
                    joinChannelUseCase(intent.channelId, intent.name)
                    _effects.emit(ChannelListEffect.NavigateToChannel(intent.channelId))
                }
            }
            is ChannelListIntent.CreateChannel -> {
                val name = _state.value.newChannelName
                if (name.isNotBlank()) {
                    scope.launch {
                        val id = createChannelUseCase(name)
                        _effects.emit(ChannelListEffect.NavigateToChannel(id))
                    }
                }
            }
        }
    }
}
