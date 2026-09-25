package com.pttlan.data.ptt.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import co.touchlab.kermit.Logger
import com.pttlan.core.audio.AudioPlayer
import com.pttlan.core.audio.TimeStretcher
import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.datastore.SettingsDefaults
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.HistoryRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

private const val PLAYBACK_CHUNK_BYTES = 4096
private const val PAUSE_POLL_MS = 100L
private const val MS_PER_SECOND = 1000L

/** Mono 16 bit PCM at 48 kHz, the format the recorder writes. */
private const val BYTES_PER_SECOND = 48_000L * 2

/** Reading, replaying and deleting recorded messages. Writing them is [HistoryRecorder]. */
class HistoryRepositoryImpl(
    private val audioPlayer: AudioPlayer,
    private val database: PttDatabase,
    private val settings: Settings,
    private val storageInfoProvider: StorageInfoProvider,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : HistoryRepository {
    private val logger = Logger.withTag("audio")
    private val scope = CoroutineScope(dispatcher)

    private val _playbackPosition = MutableStateFlow<PlaybackPosition?>(null)
    override val playbackPosition: StateFlow<PlaybackPosition?> = _playbackPosition.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var playbackJob: Job? = null
    private var isPlaybackPaused = false
    private var currentPlaybackMessageId: String? = null

    @Volatile
    private var seekRequestMs: Long? = null

    override fun getRecentMessages(channelId: String): Flow<List<VoiceMessage>> =
        database.voiceMessageQueries
            .getRecentMessagesByChannel(channelId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    VoiceMessage(
                        id = it.id,
                        channelId = it.channelId,
                        senderNickname = it.senderNickname,
                        filePath = it.filePath,
                        durationMs = it.durationMs,
                        recordedAt = it.recordedAt,
                        playedAt = it.playedAt,
                    )
                }
            }

    override fun getAllMessages(): Flow<List<VoiceMessage>> =
        database.voiceMessageQueries
            .getAllMessages()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list ->
                list.map {
                    VoiceMessage(
                        id = it.id,
                        channelId = it.channelId,
                        senderNickname = it.senderNickname,
                        filePath = it.filePath,
                        durationMs = it.durationMs,
                        recordedAt = it.recordedAt,
                        playedAt = it.playedAt,
                    )
                }
            }

    override suspend fun playMessage(message: VoiceMessage) {
        stopPlayingMessage()
        currentPlaybackMessageId = message.id
        isPlaybackPaused = false

        playbackJob =
            scope.launch {
                try {
                    val path = message.filePath.toPath()
                    val sizeBytes = fileSystem.metadataOrNull(path)?.size
                    // The recorded file is the honest total: `durationMs` is wall clock of the talk spurt
                    val totalMs = sizeBytes?.let { it * MS_PER_SECOND / BYTES_PER_SECOND } ?: message.durationMs
                    _playbackPosition.value = PlaybackPosition(message.id, 0, totalMs)

                    var source = fileSystem.source(path).buffer()
                    var stretcher = TimeStretcher()
                    val buffer = ByteArray(PLAYBACK_CHUNK_BYTES)
                    var sequenceNumber = 0
                    var playedBytes = 0L

                    while (isActive) {
                        seekRequestMs?.let { targetMs ->
                            seekRequestMs = null
                            // Drop what the player queued at the old position, then read on from the new one,
                            // on a whole 16 bit sample.
                            audioPlayer.stop()
                            source.close()
                            source = fileSystem.source(path).buffer()
                            val targetBytes = targetMs * BYTES_PER_SECOND / MS_PER_SECOND
                            playedBytes = targetBytes.coerceIn(0L, sizeBytes ?: 0L) and 1L.inv()
                            source.skip(playedBytes)
                            stretcher = TimeStretcher()
                            _playbackPosition.value =
                                PlaybackPosition(message.id, playedBytes * MS_PER_SECOND / BYTES_PER_SECOND, totalMs)
                        }
                        if (isPlaybackPaused) {
                            delay(PAUSE_POLL_MS.milliseconds)
                            continue
                        }
                        val read = source.read(buffer)
                        if (read == -1) {
                            val rest = stretcher.flush()
                            if (rest.isNotEmpty()) audioPlayer.play(rest, sequenceNumber = sequenceNumber++)
                            database.voiceMessageQueries.markPlayed(Clock.System.now().toEpochMilliseconds(), message.id)
                            break
                        }

                        // The player keeps the array in its queue, so it has to be a copy: reusing `buffer`
                        // meant the next read overwrote audio that had not been played yet.
                        // The sequence number has to advance as well, or the jitter buffer treats every chunk
                        // after the first as a late duplicate of packet 0 and drops it.
                        stretcher.speed = _playbackSpeed.value
                        val chunk = stretcher.process(buffer.copyOf(read))
                        if (chunk.isNotEmpty()) audioPlayer.play(chunk, sequenceNumber = sequenceNumber++)

                        // Feed at playback speed, which is the length of what came out of the stretcher. Reading
                        // the whole file at disk speed and returning would hit the `finally` below and stop the
                        // player while the queue was still full.
                        delay((chunk.size * MS_PER_SECOND / BYTES_PER_SECOND).milliseconds)

                        // Position comes from the bytes fed, not from a sum of per-chunk milliseconds:
                        // the integer division would drop a few ms per chunk and never reach the end.
                        playedBytes += read
                        val positionMs = (playedBytes * MS_PER_SECOND / BYTES_PER_SECOND).coerceAtMost(totalMs)
                        _playbackPosition.value = PlaybackPosition(message.id, positionMs, totalMs)
                    }
                    source.close()
                } catch (e: Exception) {
                    logger.w(e) { "Failed to play a message from the history" }
                } finally {
                    audioPlayer.stop()
                    currentPlaybackMessageId = null
                    isPlaybackPaused = false
                    _playbackPosition.value = null
                }
            }
        playbackJob?.join()
    }

    override suspend fun pausePlayingMessage() {
        if (currentPlaybackMessageId != null) {
            isPlaybackPaused = true
        }
    }

    override suspend fun resumePlayingMessage() {
        if (currentPlaybackMessageId != null) {
            isPlaybackPaused = false
        }
    }

    override suspend fun seekTo(positionMs: Long) {
        if (currentPlaybackMessageId != null) {
            seekRequestMs = positionMs
        }
    }

    override suspend fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    override suspend fun stopPlayingMessage() {
        _playbackPosition.value = null
        playbackJob?.cancel()
        playbackJob = null
        isPlaybackPaused = false
        seekRequestMs = null
        currentPlaybackMessageId = null
        audioPlayer.stop()
    }

    override suspend fun clearAllMessages() {
        stopPlayingMessage()
        database.voiceMessageQueries.deleteAllMessages()

        try {
            val cacheLocation = settings.getString(SettingsKeys.CACHE_LOCATION, SettingsDefaults.CACHE_LOCATION)
            val dirPath = storageInfoProvider.getCacheDirPath(cacheLocation) ?: return
            val dir = dirPath.toPath()
            val files = fileSystem.list(dir).filter { it.name.endsWith(".pcm") }
            for (file in files) {
                fileSystem.delete(file)
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to delete audio files" }
        }
    }

    override suspend fun deleteMessage(message: VoiceMessage) {
        if (currentPlaybackMessageId == message.id) {
            stopPlayingMessage()
        }
        database.voiceMessageQueries.deleteById(message.id)
        try {
            val path = message.filePath.toPath()
            fileSystem.delete(path)
        } catch (e: Exception) {
            logger.w(e) { "Failed to delete the message file" }
        }
    }

    override suspend fun deleteChannelMessages(channelId: String) {
        val messages = database.voiceMessageQueries.getRecentMessagesByChannel(channelId).executeAsList()
        messages.forEach { message ->
            if (currentPlaybackMessageId == message.id) {
                stopPlayingMessage()
            }
            try {
                val path = message.filePath.toPath()
                fileSystem.delete(path)
            } catch (e: Exception) {
                logger.w(e) { "Failed to delete purged files" }
            }
        }
        database.voiceMessageQueries.deleteAllByChannel(channelId)
    }
}
