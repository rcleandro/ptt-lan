package com.pttlan.data.ptt.repository

import co.touchlab.kermit.Logger
import com.pttlan.core.database.VoiceMessageQueries
import okio.FileSystem
import okio.Path.Companion.toPath

/** History kept per channel; older messages are purged as new ones arrive. */
const val MAX_MESSAGES_PER_CHANNEL = 50L

/**
 * Deletes the oldest messages of a channel, files first and rows after. Deleting only the rows — what the
 * code did before — left the `.pcm` files on disk until the cache size limit happened to sweep them.
 */
@Suppress("TooGenericExceptionCaught")
fun purgeOldestMessages(
    queries: VoiceMessageQueries,
    fileSystem: FileSystem,
    channelId: String,
    toDelete: Long,
) {
    if (toDelete <= 0) return
    val logger = Logger.withTag("audio")

    queries.getOldestMessagesByChannel(channelId, toDelete).executeAsList().forEach { message ->
        try {
            fileSystem.delete(message.filePath.toPath())
        } catch (e: Exception) {
            logger.w(e) { "Failed to delete ${message.filePath} while purging the history" }
        }
    }
    queries.deleteOldestByChannel(channelId, toDelete)
}
