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
    val currentSpeakerId: String? = null,
    val floorBlocked: Boolean = false
)

sealed interface PttIntent {
    data object PressPtt : PttIntent
    data object ReleasePtt : PttIntent
    data object LeaveChannel : PttIntent
}

sealed interface PttEffect {
    data object NavigateBack : PttEffect
    data class ShowFloorDenied(val reason: String) : PttEffect
}

class PttComponent(
    componentContext: ComponentContext,
    private val channelId: String,
    private val userId: String,
    private val voiceRepository: VoiceRepository,
    private val webSocketClient: com.pttlan.core.network.PttWebSocketClient
) : ComponentContext by componentContext {

    private val _state = MutableStateFlow(PttState(channelId = channelId))
    val state: StateFlow<PttState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PttEffect>()
    val effects: SharedFlow<PttEffect> = _effects.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main)
    
    init {
        scope.launch {
            webSocketClient.controlMessages.collect { message ->
                when (message) {
                    is com.pttlan.core.network.protocol.ControlMessage.SpeakerChanged -> {
                        if (message.channelId == channelId) {
                            _state.update { it.copy(
                                currentSpeakerId = if (message.isSpeaking) message.userId else null,
                                floorBlocked = message.isSpeaking && message.userId != userId
                            ) }
                            
                            // If we were granted the floor
                            if (message.isSpeaking && message.userId == userId) {
                                voiceRepository.startTransmitting(channelId, userId)
                            }
                        }
                    }
                    is com.pttlan.core.network.protocol.ControlMessage.FloorDenied -> {
                        if (message.channelId == channelId) {
                            _state.update { it.copy(isTransmitting = false) }
                            _effects.emit(PttEffect.ShowFloorDenied(message.reason))
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun onIntent(intent: PttIntent) {
        when (intent) {
            is PttIntent.PressPtt -> {
                if (_state.value.floorBlocked) {
                    scope.launch { _effects.emit(PttEffect.ShowFloorDenied("Canal ocupado")) }
                    return
                }
                scope.launch {
                    _state.update { it.copy(isTransmitting = true) }
                    voiceRepository.requestFloor(channelId, userId)
                }
            }
            is PttIntent.ReleasePtt -> {
                scope.launch {
                    _state.update { it.copy(isTransmitting = false) }
                    voiceRepository.stopTransmitting()
                    voiceRepository.releaseFloor(channelId, userId)
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
