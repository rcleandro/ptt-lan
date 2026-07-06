package com.pttlan.feature.channellist

import com.arkivanov.decompose.ComponentContext
import com.pttlan.domain.ptt.repository.ChannelDomain
import com.pttlan.domain.ptt.repository.ChannelRepository
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
    private val channelRepository: ChannelRepository,
) : ComponentContext by componentContext {
    private val _state = MutableStateFlow(ChannelListState())
    val state: StateFlow<ChannelListState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ChannelListEffect>()
    val effects: SharedFlow<ChannelListEffect> = _effects.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            channelRepository.getRecentChannels().collect { channels ->
                _state.update { it.copy(recentChannels = channels) }
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
                    channelRepository.saveChannel(ChannelDomain(intent.channelId, intent.name, false))
                    _effects.emit(ChannelListEffect.NavigateToChannel(intent.channelId))
                }
            }
            is ChannelListIntent.CreateChannel -> {
                val name = _state.value.newChannelName
                if (name.isNotBlank()) {
                    val id = name.lowercase().replace(" ", "-")
                    scope.launch {
                        channelRepository.saveChannel(ChannelDomain(id, name, false))
                        _effects.emit(ChannelListEffect.NavigateToChannel(id))
                    }
                }
            }
        }
    }
}
