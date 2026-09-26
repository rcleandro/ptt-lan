package com.pttlan.data.ptt.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.common.storage.StorageOption
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.datastore.SettingsKeys
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val CACHE_DIR = "/cache"

private class FakeStorageInfoProvider : StorageInfoProvider {
    override val isExternalStorageSupported = false

    override fun getAvailableStorageOptions(): List<StorageOption> = emptyList()

    override fun getCacheUsageBytes(cacheLocationId: String): Long = 0

    override fun clearCache(cacheLocationId: String) = Unit

    override fun getCacheDirPath(cacheLocationId: String): String = CACHE_DIR
}

class HistoryRecorderTest {
    private val fileSystem = FakeFileSystem().also { it.createDirectories(CACHE_DIR.toPath()) }
    private val database =
        PttDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PttDatabase.Schema.create(it) })
    private val settings = MapSettings()

    private fun recorder() =
        HistoryRecorder(
            database = database,
            settings = settings,
            storageInfoProvider = FakeStorageInfoProvider(),
            fileSystem = fileSystem,
            dispatcher = Dispatchers.Unconfined,
        )

    @Test
    fun recordsATalkSpurtWhenTheCacheIsEnabled() =
        runTest {
            settings.putBoolean(SettingsKeys.ALLOW_CACHE, true)
            val recorder = recorder()

            recorder.onSpeakerStarted(channelId = "c1", userId = "u1", nickname = "Tester")
            recorder.write(ByteArray(64) { 1 })
            recorder.onSpeakerStopped("u1")

            val messages = database.voiceMessageQueries.getRecentMessagesByChannel("c1").executeAsList()
            assertEquals(1, messages.size)
            assertEquals("Tester", messages.first().senderNickname)
            assertTrue(fileSystem.exists(messages.first().filePath.toPath()))
        }

    @Test
    fun recordsNothingWhenTheCacheIsDisabled() =
        runTest {
            settings.putBoolean(SettingsKeys.ALLOW_CACHE, false)
            val recorder = recorder()

            recorder.onSpeakerStarted(channelId = "c1", userId = "u1", nickname = "Tester")
            recorder.write(ByteArray(64) { 1 })
            recorder.onSpeakerStopped("u1")

            assertEquals(0, database.voiceMessageQueries.countByChannel("c1").executeAsOne())
            assertTrue(fileSystem.list(CACHE_DIR.toPath()).isEmpty())
        }

    @Test
    fun dropsAnEmptyRecordingInsteadOfStoringIt() =
        runTest {
            settings.putBoolean(SettingsKeys.ALLOW_CACHE, true)
            val recorder = recorder()

            recorder.onSpeakerStarted(channelId = "c1", userId = "u1", nickname = "Tester")
            recorder.onSpeakerStopped("u1")

            assertEquals(0, database.voiceMessageQueries.countByChannel("c1").executeAsOne())
            assertTrue(fileSystem.list(CACHE_DIR.toPath()).isEmpty(), "an empty file must not be kept")
        }

    @Test
    fun recordsOpusFramesAFractionOfTheSizeOfPcm() =
        runTest {
            // 31.4: a second of speech was 96 KB of PCM (5.8 MB a minute); the history now keeps Opus frames
            settings.putBoolean(SettingsKeys.ALLOW_CACHE, true)
            val recorder = recorder()
            val second = toneSecond()

            recorder.onSpeakerStarted(channelId = "c1", userId = "u1", nickname = "Tester")
            second.toList().chunked(PCM_FRAME_BYTES).forEach { recorder.write(it.toByteArray()) }
            recorder.onSpeakerStopped("u1")

            val path =
                database.voiceMessageQueries
                    .getRecentMessagesByChannel("c1")
                    .executeAsOne()
                    .filePath
            assertTrue(path.endsWith(".opf"), "got $path")
            val size = fileSystem.metadata(path.toPath()).size!!
            assertTrue(size < second.size / 5, "expected well under a fifth of ${second.size} bytes, got $size")
        }
}

/** 20 ms of 48 kHz mono 16 bit PCM, the chunk the app captures and receives. */
internal const val PCM_FRAME_BYTES = 1920

/** One second of a 440 Hz tone, as 48 kHz mono 16 bit little-endian PCM. */
internal fun toneSecond(): ByteArray =
    ByteArray(96_000).also { pcm ->
        for (i in 0 until 48_000) {
            val sample = (sin(2 * PI * 440 * i / 48_000) * 8_000).toInt()
            pcm[2 * i] = sample.toByte()
            pcm[2 * i + 1] = (sample shr 8).toByte()
        }
    }
