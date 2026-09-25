package com.pttlan.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The first migration: an app updated from version 1 keeps its history and gains `playedAt`. */
class VoiceMessageMigrationTest {
    @Test
    fun aVersion1HistoryKeepsItsMessagesAsNotPlayed() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // The schema as version 1 shipped it
        driver.execute(
            null,
            """
            CREATE TABLE VoiceMessage (
                id TEXT NOT NULL PRIMARY KEY,
                channelId TEXT NOT NULL,
                senderNickname TEXT NOT NULL,
                filePath TEXT NOT NULL,
                durationMs INTEGER NOT NULL,
                recordedAt INTEGER NOT NULL
            )
            """.trimIndent(),
            0,
        )
        driver.execute(null, "INSERT INTO VoiceMessage VALUES ('m1', 'Geral', 'Ana', '/m1.pcm', 1000, 10)", 0)

        PttDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = PttDatabase.Schema.version)

        val queries = PttDatabase(driver).voiceMessageQueries
        val message = queries.getAllMessages().executeAsOne()
        assertEquals("m1", message.id)
        assertNull(message.playedAt)
        queries.markPlayed(playedAt = 99, id = "m1")
        assertEquals(99, queries.getAllMessages().executeAsOne().playedAt)
    }
}
