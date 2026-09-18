package com.pttlan.feature.history

import com.arkivanov.decompose.ComponentContext
import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.HistoryRepository
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
    private val historyRepository: HistoryRepository,
    private val onBackClicked: () -> Unit,
) : ComponentContext by componentContext {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _messages = MutableStateFlow<List<VoiceMessage>>(emptyList())
    val messages: StateFlow<List<VoiceMessage>> = _messages.asStateFlow()

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _isPaused = MutableStateFlow<Boolean>(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    /** Progress of the message being replayed, straight from the repository. */
    val playbackPosition: StateFlow<PlaybackPosition?> = historyRepository.playbackPosition

    init {
        historyRepository
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
                    historyRepository.resumePlayingMessage()
                } else {
                    _isPaused.value = true
                    historyRepository.pausePlayingMessage()
                }
            } else {
                _playingMessageId.value = message.id
                _isPaused.value = false
                historyRepository.playMessage(message)
                if (_playingMessageId.value == message.id) {
                    _playingMessageId.value = null
                    _isPaused.value = false
                }
            }
        }
    }

    fun stopPlaying() {
        scope.launch {
            historyRepository.stopPlayingMessage()
            _playingMessageId.value = null
            _isPaused.value = false
        }
    }

    fun clearAllMessages() {
        scope.launch {
            historyRepository.clearAllMessages()
        }
    }

    fun deleteMessage(message: VoiceMessage) {
        scope.launch {
            historyRepository.deleteMessage(message)
        }
    }

    fun deleteChannelMessages(channelId: String) {
        scope.launch {
            historyRepository.deleteChannelMessages(channelId)
        }
    }
}
