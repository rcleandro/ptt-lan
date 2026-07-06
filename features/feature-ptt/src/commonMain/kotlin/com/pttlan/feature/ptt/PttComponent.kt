package com.pttlan.feature.ptt

import com.arkivanov.decompose.ComponentContext
import com.pttlan.domain.ptt.repository.VoiceRepository
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

data class PttState(
    val channelId: String = "",
    val isTransmitting: Boolean = false,
    val currentSpeakerId: String? = null
)

sealed interface PttIntent {
    data object PressPtt : PttIntent
    data object ReleasePtt : PttIntent
    data object LeaveChannel : PttIntent
}

sealed interface PttEffect {
    data object NavigateBack : PttEffect
}

class PttComponent(
    componentContext: ComponentContext,
    private val channelId: String,
    private val userId: String,
    private val voiceRepository: VoiceRepository
) : ComponentContext by componentContext {

    private val _state = MutableStateFlow(PttState(channelId = channelId))
    val state: StateFlow<PttState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PttEffect>()
    val effects: SharedFlow<PttEffect> = _effects.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    fun onIntent(intent: PttIntent) {
        when (intent) {
            is PttIntent.PressPtt -> {
                scope.launch {
                    _state.update { it.copy(isTransmitting = true) }
                    voiceRepository.startTransmitting(channelId, userId)
                }
            }
            is PttIntent.ReleasePtt -> {
                scope.launch {
                    _state.update { it.copy(isTransmitting = false) }
                    voiceRepository.stopTransmitting()
                }
            }
            is PttIntent.LeaveChannel -> {
                scope.launch {
                    _effects.emit(PttEffect.NavigateBack)
                }
            }
        }
    }
}
