package com.pttlan.data.ptt.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pttlan.core.database.PttDatabase
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VoiceMessagePurgeTest {
    private val fileSystem = FakeFileSystem()
    private val database =
        PttDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PttDatabase.Schema.create(it) })

    private fun record(
        id: String,
        recordedAt: Long,
    ): String {
        val path = "/cache/$id.pcm"
        fileSystem.createDirectories("/cache".toPath())
        fileSystem.write(path.toPath()) { writeUtf8("audio") }
        database.voiceMessageQueries.insert(
            id = id,
            channelId = "c1",
            senderNickname = "Tester",
            filePath = path,
            durationMs = 1_000,
            recordedAt = recordedAt,
        )
        return path
    }

    @Test
    fun deletesTheFilesOfThePurgedMessages() {
        val oldest = record("m1", recordedAt = 1)
        val middle = record("m2", recordedAt = 2)
        val newest = record("m3", recordedAt = 3)

        purgeOldestMessages(database.voiceMessageQueries, fileSystem, channelId = "c1", toDelete = 2)

        assertFalse(fileSystem.exists(oldest.toPath()), "the oldest file must be gone")
        assertFalse(fileSystem.exists(middle.toPath()), "the second oldest file must be gone")
        assertTrue(fileSystem.exists(newest.toPath()), "the newest file must be kept")
        assertEquals(1, database.voiceMessageQueries.countByChannel("c1").executeAsOne())
    }

    @Test
    fun stillDropsTheRowWhenTheFileIsAlreadyGone() {
        val path = record("m1", recordedAt = 1)
        fileSystem.delete(path.toPath())

        purgeOldestMessages(database.voiceMessageQueries, fileSystem, channelId = "c1", toDelete = 1)

        assertEquals(0, database.voiceMessageQueries.countByChannel("c1").executeAsOne())
    }
}
