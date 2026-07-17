package com.pttlan.feature.history

import com.arkivanov.decompose.ComponentContext
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.VoiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HistoryComponent(
    componentContext: ComponentContext,
    private val voiceRepository: VoiceRepository,
    private val onBackClicked: () -> Unit,
) : ComponentContext by componentContext {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _messages = MutableStateFlow<List<VoiceMessage>>(emptyList())
    val messages: StateFlow<List<VoiceMessage>> = _messages.asStateFlow()

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _isPaused = MutableStateFlow<Boolean>(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    init {
        voiceRepository
            .getAllMessages()
            .onEach { _messages.value = it }
            .launchIn(scope)
    }

    fun onBack() {
        onBackClicked()
    }

    fun playMessage(message: VoiceMessage) {
        scope.launch {
            if (_playingMessageId.value == message.id) {
                if (_isPaused.value) {
                    _isPaused.value = false
                    voiceRepository.resumePlayingMessage()
                } else {
                    _isPaused.value = true
                    voiceRepository.pausePlayingMessage()
                }
            } else {
                _playingMessageId.value = message.id
                _isPaused.value = false
                voiceRepository.playMessage(message)
                if (_playingMessageId.value == message.id) {
                    _playingMessageId.value = null
                    _isPaused.value = false
                }
            }
        }
    }

    fun stopPlaying() {
        scope.launch {
            voiceRepository.stopPlayingMessage()
            _playingMessageId.value = null
            _isPaused.value = false
        }
    }

    fun clearAllMessages() {
        scope.launch {
            voiceRepository.clearAllMessages()
        }
    }

    fun deleteMessage(message: VoiceMessage) {
        scope.launch {
            voiceRepository.deleteMessage(message)
        }
    }
}
