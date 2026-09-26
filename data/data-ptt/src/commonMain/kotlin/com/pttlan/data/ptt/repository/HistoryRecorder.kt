package com.pttlan.data.ptt.repository

import co.touchlab.kermit.Logger
import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.datastore.SettingsDefaults
import com.pttlan.core.datastore.SettingsKeys
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import kotlin.time.Clock

private const val BYTES_PER_MB = 1024L * 1024L

/**
 * Writes the audio of a talk spurt to the cache and keeps that cache within its limits.
 *
 * Every entry point runs on a single-threaded dispatcher: the open sink, the current speaker and the file
 * path used to be plain fields mutated from the control-message coroutine, the reception coroutine and the
 * transmission coroutine at the same time, all on `Dispatchers.Default`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRecorder(
    private val database: PttDatabase,
    private val settings: Settings,
    private val storageInfoProvider: StorageInfoProvider,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1),
) {
    private val logger = Logger.withTag("audio")

    private var speakerId: String? = null
    private var speakerNickname: String? = null
    private var channelId: String? = null
    private var startedAtMs: Long = 0
    private var writer: OpusFrameWriter? = null
    private var filePath: String? = null

    /** Opens a file for the new talk spurt, when the user enabled the cache. */
    suspend fun onSpeakerStarted(
        channelId: String,
        userId: String,
        nickname: String,
    ) = withContext(dispatcher) {
        speakerId = userId
        speakerNickname = nickname
        this@HistoryRecorder.channelId = channelId
        startedAtMs = Clock.System.now().toEpochMilliseconds()

        if (!settings.getBoolean(SettingsKeys.ALLOW_CACHE, SettingsDefaults.ALLOW_CACHE)) return@withContext

        val cacheLocation = settings.getString(SettingsKeys.CACHE_LOCATION, SettingsDefaults.CACHE_LOCATION)
        val dirPath = storageInfoProvider.getCacheDirPath(cacheLocation) ?: return@withContext
        val path = "$dirPath/${channelId}_$startedAtMs.$OPUS_EXTENSION".toPath()
        filePath = path.toString()
        try {
            writer = OpusFrameWriter(fileSystem.sink(path).buffer())
        } catch (e: Exception) {
            logger.w(e) { "Failed to open the recording file" }
        }
    }

    /** Closes the file and records the message, unless nothing was actually written. */
    suspend fun onSpeakerStopped(userId: String) =
        withContext(dispatcher) {
            writer?.close()
            writer = null

            val path = filePath
            val channel = channelId
            if (speakerId == userId && channel != null && path != null) {
                if (recordedSize(path) == 0L) {
                    runCatching { fileSystem.delete(path.toPath()) }
                } else {
                    store(channel, path)
                }
            }

            speakerId = null
            speakerNickname = null
            filePath = null
            channelId = null
        }

    /** Appends decoded audio to the open recording, encoded to Opus as whole frames fill up. */
    suspend fun write(chunk: ByteArray) =
        withContext(dispatcher) {
            try {
                writer?.write(chunk)
            } catch (e: Exception) {
                logger.w(e) { "Failed to store audio" }
            }
        }

    private fun recordedSize(path: String): Long =
        try {
            fileSystem.metadataOrNull(path.toPath())?.size ?: 0L
        } catch (e: Exception) {
            logger.w(e) { "Failed to read the size of $path" }
            0L
        }

    private fun store(
        channel: String,
        path: String,
    ) {
        val queries = database.voiceMessageQueries
        queries.insert(
            id = "${channel}_$startedAtMs",
            channelId = channel,
            senderNickname = speakerNickname ?: speakerId.orEmpty(),
            filePath = path,
            durationMs = Clock.System.now().toEpochMilliseconds() - startedAtMs,
            recordedAt = startedAtMs,
        )

        val count = queries.countByChannel(channel).executeAsOne()
        if (count > MAX_MESSAGES_PER_CHANNEL) {
            purgeOldestMessages(
                queries = queries,
                fileSystem = fileSystem,
                channelId = channel,
                toDelete = count - MAX_MESSAGES_PER_CHANNEL,
            )
        }
        trimCacheBySize()
    }

    /** Deletes the oldest files until the cache fits the size the user chose. */
    private fun trimCacheBySize() {
        try {
            val limitBytes = settings.getInt(SettingsKeys.MAX_CACHE_SIZE_MB, SettingsDefaults.MAX_CACHE_SIZE_MB) * BYTES_PER_MB
            val cacheLocation = settings.getString(SettingsKeys.CACHE_LOCATION, SettingsDefaults.CACHE_LOCATION)
            val dirPath = storageInfoProvider.getCacheDirPath(cacheLocation) ?: return

            val files = fileSystem.list(dirPath.toPath()).filter(::isRecording)
            var totalSize = files.sumOf { fileSystem.metadata(it).size ?: 0L }
            if (totalSize <= limitBytes) return

            files.sortedBy { fileSystem.metadata(it).lastModifiedAtMillis ?: 0L }.forEach { file ->
                if (totalSize <= limitBytes) return
                val size = fileSystem.metadata(file).size ?: 0L
                fileSystem.delete(file)
                database.voiceMessageQueries.deleteByFilePath(file.toString())
                totalSize -= size
            }
        } catch (e: Exception) {
            logger.w(e) { "Failed to trim the history by size" }
        }
    }
}
