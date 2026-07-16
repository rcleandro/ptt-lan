package com.pttlan.feature.ptt

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.pttlan.core.network.protocol.ParticipantDto
import com.pttlan.domain.ptt.repository.VoiceRepository
import com.pttlan.domain.ptt.usecase.JoinChannelUseCase
import com.pttlan.domain.ptt.usecase.LeaveChannelUseCase
import com.pttlan.domain.ptt.usecase.ObserveFloorDeniedUseCase
import com.pttlan.domain.ptt.usecase.ObserveParticipantsUseCase
import com.pttlan.domain.ptt.usecase.ObserveSpeakerUseCase
import com.pttlan.domain.ptt.usecase.StartTransmittingUseCase
import com.pttlan.domain.ptt.usecase.StopTransmittingUseCase
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class PttState(
    val channelId: String = "",
    val localUserId: String = "",
    val isTransmitting: Boolean = false,
    val currentSpeakerId: String? = null,
    val currentSpeakerName: String? = null,
    val floorBlocked: Boolean = false,
    val isFloorGranted: Boolean = false,
    val participants: List<ParticipantDto> = emptyList(),
)

sealed interface PttIntent {
    data object PressPtt : PttIntent

    data object ReleasePtt : PttIntent

    data object LeaveChannel : PttIntent
}

sealed interface PttEffect {
    data object NavigateBack : PttEffect

    data class ShowFloorDenied(
        val reason: String,
    ) : PttEffect
}

class PttComponent(
    componentContext: ComponentContext,
    private val channelId: String,
    private val userId: String,
    private val voiceRepository: VoiceRepository,
    private val joinChannelUseCase: JoinChannelUseCase,
    private val leaveChannelUseCase: LeaveChannelUseCase,
    private val observeParticipantsUseCase: ObserveParticipantsUseCase,
    private val observeSpeakerUseCase: ObserveSpeakerUseCase,
    private val observeFloorDeniedUseCase: ObserveFloorDeniedUseCase,
    private val startTransmittingUseCase: StartTransmittingUseCase,
    private val stopTransmittingUseCase: StopTransmittingUseCase,
) : ComponentContext by componentContext,
    KoinComponent {
    private val settings: Settings by inject()

    private val _state = MutableStateFlow(PttState(channelId = channelId, localUserId = userId))
    val state: StateFlow<PttState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<PttEffect>()
    val effects: SharedFlow<PttEffect> = _effects.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        scope.launch {
            val nickname = settings.getString("nickname", "User-${userId.take(4)}")
            joinChannelUseCase(channelId = channelId, userId = userId, nickname = nickname)
        }

        scope.launch {
            observeSpeakerUseCase(channelId).collect { speakerState ->
                val isMe = speakerState.userId == userId
                _state.update {
                    it.copy(
                        currentSpeakerId = if (speakerState.isSpeaking) speakerState.userId else null,
                        currentSpeakerName = if (speakerState.isSpeaking) speakerState.nickname else null,
                        floorBlocked = speakerState.isSpeaking && !isMe,
                        isFloorGranted = speakerState.isSpeaking && isMe,
                    )
                }

                // If we were granted the floor
                if (speakerState.isSpeaking && speakerState.userId == userId) {
                    voiceRepository.startTransmitting(channelId, userId)
                }
            }
        }

        scope.launch {
            observeFloorDeniedUseCase(channelId).collect { reason ->
                _state.update { it.copy(isTransmitting = false) }
                _effects.emit(PttEffect.ShowFloorDenied(reason))
            }
        }

        scope.launch {
            observeParticipantsUseCase(channelId).collect { participants ->
                val dtos =
                    participants.map {
                        ParticipantDto(it.userId, it.nickname, it.isSpeaking)
                    }
                _state.update { it.copy(participants = dtos) }
            }
        }

        lifecycle.subscribe(
            object : Lifecycle.Callbacks {
                override fun onDestroy() {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            leaveChannelUseCase(channelId = channelId, userId = userId)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    scope.cancel()
                }
            },
        )
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
                    startTransmittingUseCase(channelId, userId)
                }
            }
            is PttIntent.ReleasePtt -> {
                scope.launch {
                    _state.update { it.copy(isTransmitting = false, isFloorGranted = false) }
                    stopTransmittingUseCase(channelId, userId)
                }
            }
            is PttIntent.LeaveChannel -> {
                scope.launch {
                    leaveChannelUseCase(channelId, userId)
                    _effects.emit(PttEffect.NavigateBack)
                }
            }
        }
    }
}
