package com.pttlan.data.ptt.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pttlan.core.audio.wavHeader
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Where [NoStorageInfoProvider] keeps the recordings. */
private const val OPUS_CACHE_DIR = "/cache"

/** 31.4: the history records Opus frames; replay, seek and export read them as PCM. */
@OptIn(ExperimentalCoroutinesApi::class)
class OpusHistoryTest {
    private val fileSystem = FakeFileSystem().also { it.createDirectories(OPUS_CACHE_DIR.toPath()) }
    private val database =
        PttDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PttDatabase.Schema.create(it) })
    private val player = RecordingAudioPlayer()
    private val message = VoiceMessage("m1", "c1", "Tester", MESSAGE_PATH, 1_000, 1)

    /** Records [toneSecond] through the real recorder, so the replay reads what the app writes. */
    private suspend fun recordOpusSecond(): VoiceMessage {
        val settings = MapSettings().apply { putBoolean(SettingsKeys.ALLOW_CACHE, true) }
        val recorder =
            HistoryRecorder(database, settings, NoStorageInfoProvider(), fileSystem, Dispatchers.Unconfined)
        recorder.onSpeakerStarted(channelId = "c1", userId = "u1", nickname = "Tester")
        toneSecond().toList().chunked(PCM_FRAME_BYTES).forEach { recorder.write(it.toByteArray()) }
        recorder.onSpeakerStopped("u1")
        val row =
            database.voiceMessageQueries
                .getAllMessages()
                .executeAsList()
                .last()
        return message.copy(id = row.id, filePath = row.filePath)
    }

    private fun repository(scheduler: TestCoroutineScheduler) =
        HistoryRepositoryImpl(
            audioPlayer = player,
            database = database,
            settings = MapSettings(),
            storageInfoProvider = NoStorageInfoProvider(),
            fileSystem = fileSystem,
            dispatcher = StandardTestDispatcher(scheduler),
        )

    @Test
    fun playsAnOpusRecordingToTheEnd() =
        runTest {
            val opus = recordOpusSecond()
            val repository = repository(testScheduler)
            val seen = mutableListOf<PlaybackPosition>()
            backgroundScope.launch { repository.playbackPosition.collect { it?.let(seen::add) } }

            repository.playMessage(opus)

            val fed = player.chunks.sumOf { it.first.size }
            assertTrue(fed in 96_000..96_000 + PCM_FRAME_BYTES, "a second of audio reaches the player, got $fed bytes")
            assertEquals(1_000, seen.first().durationMs)
            assertTrue(seen.last().positionMs >= 1_000 - 20, "progress reaches the end, got ${seen.last()}")
        }

    @Test
    fun seeksWithinAnOpusRecording() =
        runTest {
            val opus = recordOpusSecond()
            val repository = repository(testScheduler)
            val playing = launch { repository.playMessage(opus) }
            runCurrent()
            val before = player.chunks.size

            repository.seekTo(500)
            advanceTimeBy(20)
            runCurrent()
            assertEquals(500, repository.playbackPosition.value?.positionMs)
            playing.join()

            val afterSeek = player.chunks.drop(before).sumOf { it.first.size }
            assertTrue(afterSeek in 48_000 - PCM_FRAME_BYTES..48_000 + PCM_FRAME_BYTES, "half a second after the seek, got $afterSeek")
        }

    @Test
    fun exportsAnOpusRecordingAsAWavOfItsAudio() =
        runTest {
            val opus = recordOpusSecond()
            fileSystem.createDirectories("/shared".toPath())

            val path = repository(testScheduler).exportAsWav(opus, "/shared")!!

            val wav = fileSystem.read(path.toPath()) { readByteArray() }
            assertTrue(path.endsWith(".wav"))
            assertEquals(44 + 96_000, wav.size)
            assertContentEquals(wavHeader(96_000), wav.copyOfRange(0, 44))
        }
}
