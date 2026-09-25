package com.pttlan.data.ptt.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.pttlan.core.audio.AudioPlayer
import com.pttlan.core.audio.JitterBufferPolicy
import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.common.storage.StorageOption
import com.pttlan.core.database.PttDatabase
import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val CACHE_DIR = "/cache"
private const val MESSAGE_PATH = "/cache/c1_1.pcm"

/** One 4096 byte chunk at 48 kHz mono 16 bit. */
private const val CHUNK_MS = 43L

private class RecordingAudioPlayer : AudioPlayer {
    val chunks = mutableListOf<Pair<ByteArray, Int>>()
    var stopped = false

    override fun play(
        chunk: ByteArray,
        sampleRate: Int,
        sequenceNumber: Int,
        timestampMs: Long,
    ) {
        chunks += chunk to sequenceNumber
    }

    override fun stop() {
        stopped = true
    }
}

private class NoStorageInfoProvider : StorageInfoProvider {
    override val isExternalStorageSupported = false

    override fun getAvailableStorageOptions(): List<StorageOption> = emptyList()

    override fun getCacheUsageBytes(cacheLocationId: String): Long = 0

    override fun clearCache(cacheLocationId: String) = Unit

    override fun getCacheDirPath(cacheLocationId: String): String = CACHE_DIR
}

/**
 * Replay used to stop after the first 4096 byte chunk: every chunk was offered with sequence number 0, so the
 * jitter buffer dropped all of them as late duplicates, and the reader raced to the end of the file and
 * stopped the player. On a real device that was ~43 ms of audio out of a whole message.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryPlaybackTest {
    private val fileSystem = FakeFileSystem().also { it.createDirectories(CACHE_DIR.toPath()) }
    private val database =
        PttDatabase(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PttDatabase.Schema.create(it) })
    private val player = RecordingAudioPlayer()

    private val recorded = ByteArray(4096 * 3 + 100) { (it % 251).toByte() }

    private val message =
        VoiceMessage(
            id = "m1",
            channelId = "c1",
            senderNickname = "Tester",
            filePath = MESSAGE_PATH,
            durationMs = 1_000,
            recordedAt = 1,
        )

    @Test
    fun playsTheWholeMessageInOrder() =
        runTest {
            fileSystem.write(MESSAGE_PATH.toPath()) { write(recorded) }
            val repository =
                HistoryRepositoryImpl(
                    audioPlayer = player,
                    database = database,
                    settings = MapSettings(),
                    storageInfoProvider = NoStorageInfoProvider(),
                    fileSystem = fileSystem,
                    dispatcher = StandardTestDispatcher(testScheduler),
                )

            repository.playMessage(message)

            assertEquals(4, player.chunks.size, "every chunk of the file must reach the player")
            assertContentEquals(recorded, player.chunks.flatMap { it.first.toList() }.toByteArray())
            assertEquals(listOf(0, 1, 2, 3), player.chunks.map { it.second })
            assertTrue(player.stopped, "the player is stopped once the message ends")
        }

    @Test
    fun reportsTheProgressOfTheReplay() =
        runTest {
            fileSystem.write(MESSAGE_PATH.toPath()) { write(recorded) }
            val repository =
                HistoryRepositoryImpl(
                    audioPlayer = player,
                    database = database,
                    settings = MapSettings(),
                    storageInfoProvider = NoStorageInfoProvider(),
                    fileSystem = fileSystem,
                    dispatcher = StandardTestDispatcher(testScheduler),
                )
            val seen = mutableListOf<PlaybackPosition>()
            backgroundScope.launch {
                repository.playbackPosition.collect { it?.let(seen::add) }
            }

            repository.playMessage(message)

            assertTrue(seen.isNotEmpty(), "the progress must be reported while playing")
            assertTrue(seen.map { it.positionMs } == seen.map { it.positionMs }.sorted(), "progress only moves forward")
            assertEquals(message.id, seen.first().messageId)
            // StateFlow conflates, so the very last value may be swallowed by the reset that follows it;
            // what matters is that progress got to the end, within one chunk
            val last = seen.last()
            assertTrue(
                last.positionMs >= last.durationMs - CHUNK_MS,
                "progress should reach the end, got ${last.positionMs} of ${last.durationMs}",
            )
            assertEquals(null, repository.playbackPosition.value, "progress is cleared once the message ends")
        }

    @Test
    fun theJitterBufferKeepsEveryChunkOfAReplay() {
        val policy = JitterBufferPolicy()

        // The sequence numbers the repository now emits, checked against the policy that consumes them
        val played = (0 until 4).count { policy.shouldPlay(it) }

        assertEquals(4, played)
    }

    @Test
    fun seekingGoesOnFromTheNewPosition() =
        runTest {
            // One second at 48 kHz mono 16 bit: 96 bytes per ms
            val second = ByteArray(96_000) { (it % 251).toByte() }
            fileSystem.write(MESSAGE_PATH.toPath()) { write(second) }
            val repository =
                HistoryRepositoryImpl(
                    audioPlayer = player,
                    database = database,
                    settings = MapSettings(),
                    storageInfoProvider = NoStorageInfoProvider(),
                    fileSystem = fileSystem,
                    dispatcher = StandardTestDispatcher(testScheduler),
                )
            val playing = launch { repository.playMessage(message) }
            runCurrent()
            assertEquals(1, player.chunks.size)

            repository.seekTo(500)
            // The seek applies once the chunk being fed is done
            advanceTimeBy(CHUNK_MS)
            runCurrent()
            assertTrue(player.stopped, "what was queued at the old position must not play")
            assertEquals(500, repository.playbackPosition.value?.positionMs)
            playing.join()

            val afterSeek =
                player.chunks
                    .drop(1)
                    .flatMap { it.first.toList() }
                    .toByteArray()
            assertContentEquals(second.copyOfRange(48_000, second.size), afterSeek)
        }
}
