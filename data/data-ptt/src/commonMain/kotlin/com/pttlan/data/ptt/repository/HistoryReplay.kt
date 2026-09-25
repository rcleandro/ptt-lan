package com.pttlan.data.ptt.repository

import co.touchlab.kermit.Logger
import com.pttlan.core.audio.AudioPlayer
import com.pttlan.core.audio.TimeStretcher
import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds

private const val PLAYBACK_CHUNK_BYTES = 4096
private const val PAUSE_POLL_MS = 100L
private const val MS_PER_SECOND = 1000L

/** How far back the replay resumes after live speech interrupted it, for context. */
private const val LIVE_REWIND_MS = 1_000L

/** How long after the channel frees up the replay waits, so the end of the live audio plays alone. */
private const val LIVE_TAIL_MS = 500L

/** Mono 16 bit PCM at 48 kHz, the format the recorder writes. */
private const val BYTES_PER_SECOND = 48_000L * 2

private fun bytesToMs(bytes: Long) = bytes * MS_PER_SECOND / BYTES_PER_SECOND

/**
 * Replays one recorded message at a time into [audioPlayer]: pause, seek, speed, and giving way to live
 * speech, which shares the same player. Split from [HistoryRepositoryImpl], which keeps the database and files.
 */
internal class HistoryReplay(
    private val audioPlayer: AudioPlayer,
    private val fileSystem: FileSystem,
    private val scope: CoroutineScope,
    liveSpeaking: Flow<Boolean>,
    private val onPlayedToEnd: (messageId: String) -> Unit,
) {
    private val logger = Logger.withTag("audio")

    private val _position = MutableStateFlow<PlaybackPosition?>(null)
    val position: StateFlow<PlaybackPosition?> = _position.asStateFlow()

    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    var currentMessageId: String? = null
        private set

    private var job: Job? = null
    private var isPaused = false

    @Volatile
    private var seekRequestMs: Long? = null

    @Volatile
    private var isLivePaused = false
    private var liveTailJob: Job? = null

    init {
        liveSpeaking.onEach(::onLiveSpeaking).launchIn(scope)
    }

    /** Returns when the message ends or is stopped. */
    suspend fun play(message: VoiceMessage) {
        stop()
        currentMessageId = message.id
        isPaused = false
        // A tap plays even over live speech. It also unsticks a pause whose "channel free" never came, when
        // the connection dropped mid-speech.
        isLivePaused = false

        job =
            scope.launch {
                try {
                    if (feed(message)) onPlayedToEnd(message.id)
                } catch (e: IOException) {
                    // The file went missing or unreadable, say deleted while it played
                    logger.w(e) { "Failed to play a message from the history" }
                } finally {
                    if (!isLivePaused) audioPlayer.stop()
                    currentMessageId = null
                    isPaused = false
                    _position.value = null
                }
            }
        job?.join()
    }

    /** Feeds the file at playback speed; true when it got to the end. */
    private suspend fun feed(message: VoiceMessage): Boolean {
        val file = ReplayFile(fileSystem, message.filePath.toPath(), message.durationMs)
        _position.value = PlaybackPosition(message.id, 0, file.totalMs)
        var sequenceNumber = 0
        try {
            while (currentCoroutineContext().isActive) {
                seekRequestMs?.let { targetMs ->
                    seekRequestMs = null
                    // Drop what the player queued at the old position. Paused for live speech, the player
                    // already holds only that.
                    if (!isLivePaused) audioPlayer.stop()
                    file.seek(targetMs)
                    _position.value = PlaybackPosition(message.id, file.positionMs, file.totalMs)
                }
                if (isPaused || isLivePaused) {
                    delay(PAUSE_POLL_MS.milliseconds)
                    continue
                }
                val chunk = file.read(_speed.value)
                if (chunk == null) {
                    val rest = file.flush()
                    if (rest.isNotEmpty()) audioPlayer.play(rest, sequenceNumber = sequenceNumber)
                    return true
                }
                // The sequence number has to advance, or the jitter buffer treats every chunk after the first
                // as a late duplicate of packet 0 and drops it.
                if (chunk.isNotEmpty()) audioPlayer.play(chunk, sequenceNumber = sequenceNumber++)
                // Feed at playback speed, which is the length of what came out of the stretcher. Reading the
                // whole file at disk speed and returning would stop the player while its queue was still full.
                delay(bytesToMs(chunk.size.toLong()).milliseconds)
                _position.value = PlaybackPosition(message.id, file.positionMs, file.totalMs)
            }
            return false
        } finally {
            file.close()
        }
    }

    fun pause() {
        if (currentMessageId != null) isPaused = true
    }

    fun resume() {
        if (currentMessageId != null) isPaused = false
    }

    fun seekTo(positionMs: Long) {
        if (currentMessageId != null) seekRequestMs = positionMs
    }

    fun setSpeed(speed: Float) {
        _speed.value = speed
    }

    fun stop() {
        _position.value = null
        job?.cancel()
        job = null
        isPaused = false
        seekRequestMs = null
        currentMessageId = null
        if (!isLivePaused) audioPlayer.stop()
    }

    /**
     * Someone started speaking: stop feeding, drop the replay queued in the player and rewind a little. The
     * channel is free: resume once the live tail has played.
     */
    private fun onLiveSpeaking(speaking: Boolean) {
        liveTailJob?.cancel()
        if (speaking) {
            if (currentMessageId == null || isLivePaused) return
            isLivePaused = true
            seekRequestMs = ((_position.value?.positionMs ?: 0L) - LIVE_REWIND_MS).coerceAtLeast(0L)
            // ponytail: a chunk the loop was already handing over may still slip in, ~40 ms at most
            audioPlayer.stop()
        } else if (isLivePaused) {
            liveTailJob =
                scope.launch {
                    delay(LIVE_TAIL_MS.milliseconds)
                    isLivePaused = false
                }
        }
    }
}

/** A recorded file being read for replay: where it is in the original audio, stretched to the speed asked. */
private class ReplayFile(
    private val fileSystem: FileSystem,
    private val path: Path,
    fallbackDurationMs: Long,
) {
    private val sizeBytes = fileSystem.metadataOrNull(path)?.size

    /** The recorded file is the honest total: a message's `durationMs` is wall clock of the talk spurt. */
    val totalMs = sizeBytes?.let(::bytesToMs) ?: fallbackDurationMs

    private var source = fileSystem.source(path).buffer()
    private var stretcher = TimeStretcher()
    private val buffer = ByteArray(PLAYBACK_CHUNK_BYTES)

    // From the bytes read, not a sum of per-chunk milliseconds, whose rounding would never reach the end
    private var readBytes = 0L
    val positionMs get() = bytesToMs(readBytes).coerceAtMost(totalMs)

    /** The next chunk at [speed], maybe empty while the stretcher fills up; null at the end of the file. */
    fun read(speed: Float): ByteArray? {
        val read = source.read(buffer)
        if (read == -1) return null
        readBytes += read
        stretcher.speed = speed
        // A copy: the player keeps the array in its queue, and the next read would overwrite it
        return stretcher.process(buffer.copyOf(read))
    }

    fun flush(): ByteArray = stretcher.flush()

    /** Reads on from [targetMs], on a whole 16 bit sample. */
    fun seek(targetMs: Long) {
        source.close()
        source = fileSystem.source(path).buffer()
        readBytes = (targetMs * BYTES_PER_SECOND / MS_PER_SECOND).coerceIn(0L, sizeBytes ?: 0L) and 1L.inv()
        source.skip(readBytes)
        stretcher = TimeStretcher()
    }

    fun close() = source.close()
}
