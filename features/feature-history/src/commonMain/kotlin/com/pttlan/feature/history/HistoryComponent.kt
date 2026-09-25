package com.pttlan.feature.history

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Going back after this much of a message restarts it instead of going to the previous one. */
private const val RESTART_AFTER_MS = 3_000L

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

    private val _queue = MutableStateFlow<List<VoiceMessage>>(emptyList())

    /** The room being played in sequence, oldest first; empty when a single message is playing. */
    val queue: StateFlow<List<VoiceMessage>> = _queue.asStateFlow()

    private var playJob: Job? = null

    /** Progress of the message being replayed, straight from the repository. */
    val playbackPosition: StateFlow<PlaybackPosition?> = historyRepository.playbackPosition

    init {
        val feed =
            historyRepository
                .getAllMessages()
                .onEach { messages ->
                    _messages.value = messages
                    // A deleted message leaves the queue too; deleting the one playing ends the queue.
                    _queue.value = _queue.value.filter { queued -> messages.any { it.id == queued.id } }
                }.launchIn(scope)

        // Not scope.cancel(): a delete the user just asked for has to finish. Only what outlives the screen
        // stops: the feed, and a replay that would go on with no controls over the live channel audio.
        lifecycle.doOnDestroy {
            feed.cancel()
            if (_playingMessageId.value != null) stopPlaying()
        }
    }

    fun onBack() {
        onBackClicked()
    }

    /** Plays every message of the room in the order it was recorded, one after the other. */
    fun playChannel(channelId: String) {
        val queue = _messages.value.filter { it.channelId == channelId }.sortedBy { it.recordedAt }
        if (queue.isEmpty()) return
        _queue.value = queue
        playFrom(queue.first())
    }

    fun playMessage(message: VoiceMessage) {
        if (_playingMessageId.value == message.id) {
            scope.launch {
                if (_isPaused.value) {
                    _isPaused.value = false
                    historyRepository.resumePlayingMessage()
                } else {
                    _isPaused.value = true
                    historyRepository.pausePlayingMessage()
                }
            }
            return
        }
        // A message of the room being played goes on from there; any other one plays alone.
        if (_queue.value.none { it.id == message.id }) _queue.value = emptyList()
        playFrom(message)
    }

    fun playNext() {
        val queue = _queue.value
        queue.getOrNull(playingIndex(queue) + 1)?.let(::playFrom)
    }

    /** Like a music player: the previous message in the first seconds, otherwise the current one from the start. */
    fun playPrevious() {
        val queue = _queue.value
        val index = playingIndex(queue)
        if (index < 0) return
        val elapsedMs = playbackPosition.value?.takeIf { it.messageId == queue[index].id }?.positionMs ?: 0L
        playFrom(if (elapsedMs < RESTART_AFTER_MS && index > 0) queue[index - 1] else queue[index])
    }

    private fun playingIndex(queue: List<VoiceMessage>): Int = queue.indexOfFirst { it.id == _playingMessageId.value }

    private fun playFrom(first: VoiceMessage) {
        playJob?.cancel()
        playJob =
            scope.launch {
                var next: VoiceMessage? = first
                while (next != null) {
                    val current: VoiceMessage = next
                    _playingMessageId.value = current.id
                    _isPaused.value = false
                    historyRepository.playMessage(current)
                    val queue = _queue.value
                    val index = queue.indexOfFirst { it.id == current.id }
                    next = if (index >= 0) queue.getOrNull(index + 1) else null
                }
                _playingMessageId.value = null
                _isPaused.value = false
                _queue.value = emptyList()
            }
    }

    fun stopPlaying() {
        playJob?.cancel()
        _queue.value = emptyList()
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
