package com.pttlan.feature.history

import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Going back after this much of a message restarts it instead of going to the previous one. */
private const val RESTART_AFTER_MS = 3_000L

/** The speeds the speed button steps through, back to the first after the last. */
internal val PLAYBACK_SPEEDS = listOf(1f, 1.5f, 2f)

private fun List<VoiceMessage>.indexOfId(id: String?): Int = indexOfFirst { it.id == id }

/** The history screen's player: one message or a whole room in sequence, with its controls. */
class HistoryPlayback internal constructor(
    private val historyRepository: HistoryRepository,
    private val scope: CoroutineScope,
    private val messages: StateFlow<List<VoiceMessage>>,
) {
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

    val playbackSpeed: StateFlow<Float> = historyRepository.playbackSpeed

    /** A deleted message leaves the queue too; deleting the one playing ends the queue. */
    internal fun onMessagesChanged(messages: List<VoiceMessage>) {
        _queue.value = _queue.value.filter { queued -> messages.any { it.id == queued.id } }
    }

    /** Plays every message of the room in the order it was recorded, one after the other. */
    fun playChannel(channelId: String) {
        val queue = messages.value.filter { it.channelId == channelId }.sortedBy { it.recordedAt }
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
        queue.getOrNull(queue.indexOfId(_playingMessageId.value) + 1)?.let(::playFrom)
    }

    /** Like a music player: the previous message in the first seconds, otherwise the current one from the start. */
    fun playPrevious() {
        val queue = _queue.value
        val index = queue.indexOfId(_playingMessageId.value)
        if (index < 0) return
        val elapsedMs = playbackPosition.value?.takeIf { it.messageId == queue[index].id }?.positionMs ?: 0L
        playFrom(if (elapsedMs < RESTART_AFTER_MS && index > 0) queue[index - 1] else queue[index])
    }

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
                    val index = queue.indexOfId(current.id)
                    next = if (index >= 0) queue.getOrNull(index + 1) else null
                }
                _playingMessageId.value = null
                _isPaused.value = false
                _queue.value = emptyList()
            }
    }

    fun seekTo(positionMs: Long) {
        scope.launch { historyRepository.seekTo(positionMs) }
    }

    /** 1×, 1.5×, 2× and back to 1×. */
    fun cycleSpeed() {
        val next = PLAYBACK_SPEEDS[(PLAYBACK_SPEEDS.indexOf(playbackSpeed.value) + 1) % PLAYBACK_SPEEDS.size]
        scope.launch { historyRepository.setPlaybackSpeed(next) }
    }

    fun stop() {
        playJob?.cancel()
        _queue.value = emptyList()
        scope.launch {
            historyRepository.stopPlayingMessage()
            _playingMessageId.value = null
            _isPaused.value = false
        }
    }
}
