package com.pttlan.feature.history

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.pttlan.core.common.share.FileSharer
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

private const val WAV_MIME_TYPE = "audio/wav"

class HistoryComponent(
    componentContext: ComponentContext,
    private val historyRepository: HistoryRepository,
    private val fileSharer: FileSharer,
    private val onBackClicked: () -> Unit,
) : ComponentContext by componentContext {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _messages = MutableStateFlow<List<VoiceMessage>>(emptyList())
    val messages: StateFlow<List<VoiceMessage>> = _messages.asStateFlow()

    val playback = HistoryPlayback(historyRepository, scope, messages)

    init {
        val feed =
            historyRepository
                .getAllMessages()
                .onEach { messages ->
                    _messages.value = messages
                    playback.onMessagesChanged(messages)
                }.launchIn(scope)

        // Not scope.cancel(): a delete the user just asked for has to finish. Only what outlives the screen
        // stops: the feed, and a replay that would go on with no controls over the live channel audio.
        lifecycle.doOnDestroy {
            feed.cancel()
            if (playback.playingMessageId.value != null) playback.stop()
        }
    }

    fun onBack() {
        onBackClicked()
    }

    /** Hands the message to the system's share sheet as a `.wav`. */
    fun shareMessage(message: VoiceMessage) {
        scope.launch {
            val path = historyRepository.exportAsWav(message, fileSharer.shareDirectory)
            if (path != null) fileSharer.share(path, WAV_MIME_TYPE)
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
