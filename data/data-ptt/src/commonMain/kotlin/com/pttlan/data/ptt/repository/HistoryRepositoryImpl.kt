package com.pttlan.data.ptt.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import co.touchlab.kermit.Logger
import com.pttlan.core.audio.AudioPlayer
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.time.Clock

/** Reading, replaying and deleting recorded messages. Writing them is [HistoryRecorder]; replay is [HistoryReplay]. */
class HistoryRepositoryImpl(
    audioPlayer: AudioPlayer,
    private val database: PttDatabase,
    private val settings: Settings,
    private val storageInfoProvider: StorageInfoProvider,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    /** Whether someone is speaking in the channel; live audio shares [audioPlayer] and goes first. */
    liveSpeaking: Flow<Boolean> = emptyFlow(),
) : HistoryRepository {
    private val logger = Logger.withTag("audio")

    private val replay =
        HistoryReplay(audioPlayer, fileSystem, CoroutineScope(dispatcher), liveSpeaking) { messageId ->
            database.voiceMessageQueries.markPlayed(Clock.System.now().toEpochMilliseconds(), messageId)
        }

    init {
        replay.setSpeed(settings.getFloat(SettingsKeys.PLAYBACK_SPEED, SettingsDefaults.PLAYBACK_SPEED))
    }

    override val playbackPosition: StateFlow<PlaybackPosition?> = replay.position

    override val playbackSpeed: StateFlow<Float> = replay.speed

    override fun getRecentMessages(channelId: String): Flow<List<VoiceMessage>> =
        database.voiceMessageQueries
            .getRecentMessagesByChannel(channelId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }

    override fun getAllMessages(): Flow<List<VoiceMessage>> =
        database.voiceMessageQueries
            .getAllMessages()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun playMessage(message: VoiceMessage) = replay.play(message)

    override suspend fun pausePlayingMessage() = replay.pause()

    override suspend fun resumePlayingMessage() = replay.resume()

    override suspend fun seekTo(positionMs: Long) = replay.seekTo(positionMs)

    override suspend fun setPlaybackSpeed(speed: Float) {
        replay.setSpeed(speed)
        settings.putFloat(SettingsKeys.PLAYBACK_SPEED, speed)
    }

    override suspend fun stopPlayingMessage() = replay.stop()

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
        if (replay.currentMessageId == message.id) {
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
            if (replay.currentMessageId == message.id) {
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

private fun com.pttlan.core.database.VoiceMessage.toDomain() =
    VoiceMessage(
        id = id,
        channelId = channelId,
        senderNickname = senderNickname,
        filePath = filePath,
        durationMs = durationMs,
        recordedAt = recordedAt,
        playedAt = playedAt,
    )
