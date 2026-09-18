package com.pttlan.data.ptt.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import co.touchlab.kermit.Logger
import com.pttlan.core.audio.AudioPlayer
import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.datastore.SettingsDefaults
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.HistoryRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import kotlin.time.Duration.Companion.milliseconds

/** Reading, replaying and deleting recorded messages. Writing them is [HistoryRecorder]. */
class HistoryRepositoryImpl(
    private val audioPlayer: AudioPlayer,
    private val database: PttDatabase,
    private val settings: Settings,
    private val storageInfoProvider: StorageInfoProvider,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : HistoryRepository {
    private val logger = Logger.withTag("audio")
    private val scope = CoroutineScope(Dispatchers.Default)

    private var playbackJob: Job? = null
    private var isPlaybackPaused = false
    private var currentPlaybackMessageId: String? = null

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
                    val source = fileSystem.source(path).buffer()
                    val buffer = ByteArray(4096)

                    while (isActive) {
                        if (isPlaybackPaused) {
                            delay(100.milliseconds)
                            continue
                        }
                        val read = source.read(buffer)
                        if (read == -1) break
                        val chunk = if (read == buffer.size) buffer else buffer.copyOf(read)
                        audioPlayer.play(chunk)
                    }
                    source.close()
                } catch (e: Exception) {
                    logger.w(e) { "Failed to play a message from the history" }
                } finally {
                    audioPlayer.stop()
                    currentPlaybackMessageId = null
                    isPlaybackPaused = false
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

    override suspend fun stopPlayingMessage() {
        playbackJob?.cancel()
        playbackJob = null
        isPlaybackPaused = false
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
