package com.pttlan.feature.history

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.pttlan.domain.ptt.model.PlaybackPosition
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.HistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryComponentTest {
    private val lifecycle = LifecycleRegistry()
    private val componentContext: ComponentContext = DefaultComponentContext(lifecycle)

    private val historyRepository: HistoryRepository = mockk(relaxed = true)
    private val onBackClicked: () -> Unit = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        coEvery { historyRepository.getAllMessages() } returns emptyFlow()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createComponent() =
        HistoryComponent(
            componentContext = componentContext,
            historyRepository = historyRepository,
            onBackClicked = onBackClicked,
        )

    @Test
    fun `initialization loads recent messages`() =
        runTest(testDispatcher) {
            val mockMessage = mockk<VoiceMessage>(relaxed = true)
            coEvery { mockMessage.id } returns "msg-1"
            coEvery { historyRepository.getAllMessages() } returns flowOf(listOf(mockMessage))

            val component = createComponent()
            advanceUntilIdle()

            assertEquals(listOf(mockMessage), component.messages.value)
        }

    @Test
    fun `onBack invokes onBackClicked callback`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.onBack()

            verify(exactly = 1) { onBackClicked() }
        }

    @Test
    fun `playMessage plays message and updates state`() =
        runTest(testDispatcher) {
            val component = createComponent()
            val mockMessage = mockk<VoiceMessage>(relaxed = true)
            coEvery { mockMessage.id } returns "msg-1"

            // We use an unconfined-like approach or advance time to check state during play
            // With standard dispatcher, we can capture the state before it finishes if we mock delay,
            // but since playMessage is a suspend function that might just return immediately in mocks,
            // we'll just check that it calls the repository and then clears state.

            component.playback.playMessage(mockMessage)

            // Since playMessage launches a coroutine, we can assert state if we step through,
            // but for now, we just advance to the end.
            advanceUntilIdle()

            coVerify(exactly = 1) { historyRepository.playMessage(mockMessage) }
            assertEquals(null, component.playback.playingMessageId.value)
        }

    @Test
    fun `stop stops playback and clears playing state`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.playback.stop()
            advanceUntilIdle()

            coVerify(exactly = 1) { historyRepository.stopPlayingMessage() }
            assertEquals(null, component.playback.playingMessageId.value)
        }

    @Test
    fun `leaving the screen stops the replay and the message feed`() =
        runTest(testDispatcher) {
            // Replay went on after leaving, with no controls, over the live channel audio.
            var feeding = false
            coEvery { historyRepository.getAllMessages() } returns
                flow<List<VoiceMessage>> { awaitCancellation() }
                    .onStart { feeding = true }
                    .onCompletion { feeding = false }
            coEvery { historyRepository.playMessage(any()) } coAnswers { awaitCancellation() }
            lifecycle.resume()
            val component = createComponent()
            component.playback.playMessage(mockk(relaxed = true))
            advanceUntilIdle()

            lifecycle.destroy()
            advanceUntilIdle()

            coVerify(exactly = 1) { historyRepository.stopPlayingMessage() }
            assertFalse(feeding)
        }

    private fun message(
        id: String,
        channelId: String,
        recordedAt: Long,
    ) = VoiceMessage(id, channelId, "Ana", "/$id.pcm", 1_000, recordedAt)

    private val room =
        listOf(
            message("b", "Geral", recordedAt = 20),
            message("x", "Obra", recordedAt = 15),
            message("a", "Geral", recordedAt = 10),
            message("c", "Geral", recordedAt = 30),
        )

    @Test
    fun `playing a room plays its messages in recording order and stops at the end`() =
        runTest(testDispatcher) {
            coEvery { historyRepository.getAllMessages() } returns flowOf(room)
            val played = mutableListOf<String>()
            coEvery { historyRepository.playMessage(any()) } coAnswers { played += firstArg<VoiceMessage>().id }
            val component = createComponent()
            advanceUntilIdle()

            component.playback.playChannel("Geral")
            advanceUntilIdle()

            assertEquals(listOf("a", "b", "c"), played)
            assertEquals(null, component.playback.playingMessageId.value)
            assertEquals(emptyList(), component.playback.queue.value)
        }

    @Test
    fun `the queue advances only when the current message ends`() =
        runTest(testDispatcher) {
            coEvery { historyRepository.getAllMessages() } returns flowOf(room)
            val ending = CompletableDeferred<Unit>()
            coEvery { historyRepository.playMessage(any()) } coAnswers {
                if (firstArg<VoiceMessage>().id == "a") ending.await()
            }
            val component = createComponent()
            advanceUntilIdle()

            component.playback.playChannel("Geral")
            advanceUntilIdle()
            assertEquals("a", component.playback.playingMessageId.value)
            assertEquals(
                listOf("a", "b", "c"),
                component.playback.queue.value
                    .map { it.id },
            )

            ending.complete(Unit)
            advanceUntilIdle()
            assertEquals(null, component.playback.playingMessageId.value)
        }

    @Test
    fun `tapping a message of the active queue goes on from it`() =
        runTest(testDispatcher) {
            coEvery { historyRepository.getAllMessages() } returns flowOf(room)
            val played = mutableListOf<String>()
            var holding = true
            coEvery { historyRepository.playMessage(any()) } coAnswers {
                played += firstArg<VoiceMessage>().id
                if (holding) awaitCancellation()
            }
            val component = createComponent()
            advanceUntilIdle()
            component.playback.playChannel("Geral")
            advanceUntilIdle()

            holding = false
            component.playback.playMessage(room[0])
            advanceUntilIdle()

            assertEquals(listOf("a", "b", "c"), played)
        }

    @Test
    fun `tapping a message outside the queue plays only that message`() =
        runTest(testDispatcher) {
            coEvery { historyRepository.getAllMessages() } returns flowOf(room)
            val played = mutableListOf<String>()
            var holding = true
            coEvery { historyRepository.playMessage(any()) } coAnswers {
                played += firstArg<VoiceMessage>().id
                if (holding) awaitCancellation()
            }
            val component = createComponent()
            advanceUntilIdle()
            component.playback.playChannel("Geral")
            advanceUntilIdle()

            holding = false
            component.playback.playMessage(room[1])
            advanceUntilIdle()

            assertEquals(listOf("a", "x"), played)
            assertEquals(emptyList(), component.playback.queue.value)
        }

    @Test
    fun `stopping ends the queue`() =
        runTest(testDispatcher) {
            coEvery { historyRepository.getAllMessages() } returns flowOf(room)
            val played = mutableListOf<String>()
            coEvery { historyRepository.playMessage(any()) } coAnswers {
                played += firstArg<VoiceMessage>().id
                awaitCancellation()
            }
            val component = createComponent()
            advanceUntilIdle()
            component.playback.playChannel("Geral")
            advanceUntilIdle()

            component.playback.stop()
            advanceUntilIdle()

            assertEquals(listOf("a"), played)
            assertEquals(null, component.playback.playingMessageId.value)
            assertEquals(emptyList(), component.playback.queue.value)
        }

    /** Starts "Geral" with every message held until it is replaced, and returns what was played. */
    private fun TestScope.playRoomHeld(position: MutableStateFlow<PlaybackPosition?>): Pair<HistoryComponent, List<String>> {
        coEvery { historyRepository.getAllMessages() } returns flowOf(room)
        every { historyRepository.playbackPosition } returns position
        val played = mutableListOf<String>()
        coEvery { historyRepository.playMessage(any()) } coAnswers {
            played += firstArg<VoiceMessage>().id
            awaitCancellation()
        }
        val component = createComponent()
        advanceUntilIdle()
        component.playback.playChannel("Geral")
        advanceUntilIdle()
        return component to played
    }

    @Test
    fun `next skips to the following message of the queue`() =
        runTest(testDispatcher) {
            val (component, played) = playRoomHeld(MutableStateFlow(null))

            component.playback.playNext()
            advanceUntilIdle()

            assertEquals(listOf("a", "b"), played)
            assertEquals("b", component.playback.playingMessageId.value)
        }

    @Test
    fun `next on the last message keeps playing it`() =
        runTest(testDispatcher) {
            val (component, played) = playRoomHeld(MutableStateFlow(null))
            component.playback.playNext()
            advanceUntilIdle()
            component.playback.playNext()
            advanceUntilIdle()

            component.playback.playNext()
            advanceUntilIdle()

            assertEquals(listOf("a", "b", "c"), played)
            assertEquals("c", component.playback.playingMessageId.value)
        }

    @Test
    fun `previous in the first seconds goes back to the previous message`() =
        runTest(testDispatcher) {
            val position = MutableStateFlow<PlaybackPosition?>(null)
            val (component, played) = playRoomHeld(position)
            component.playback.playNext()
            advanceUntilIdle()
            position.value = PlaybackPosition("b", positionMs = 1_000, durationMs = 9_000)

            component.playback.playPrevious()
            advanceUntilIdle()

            assertEquals(listOf("a", "b", "a"), played)
            assertEquals("a", component.playback.playingMessageId.value)
        }

    @Test
    fun `previous later in the message restarts it`() =
        runTest(testDispatcher) {
            val position = MutableStateFlow<PlaybackPosition?>(null)
            val (component, played) = playRoomHeld(position)
            component.playback.playNext()
            advanceUntilIdle()
            position.value = PlaybackPosition("b", positionMs = 5_000, durationMs = 9_000)

            component.playback.playPrevious()
            advanceUntilIdle()

            assertEquals(listOf("a", "b", "b"), played)
            assertEquals(
                listOf("a", "b", "c"),
                component.playback.queue.value
                    .map { it.id },
            )
        }

    @Test
    fun `previous on the first message restarts it`() =
        runTest(testDispatcher) {
            val (component, played) = playRoomHeld(MutableStateFlow(null))

            component.playback.playPrevious()
            advanceUntilIdle()

            assertEquals(listOf("a", "a"), played)
        }

    @Test
    fun `the speed button steps through 1x, 1_5x and 2x and back`() =
        runTest(testDispatcher) {
            val speed = MutableStateFlow(1f)
            every { historyRepository.playbackSpeed } returns speed
            coEvery { historyRepository.setPlaybackSpeed(any()) } coAnswers { speed.value = firstArg() }
            val component = createComponent()

            val seen =
                List(3) {
                    component.playback.cycleSpeed()
                    advanceUntilIdle()
                    speed.value
                }

            assertEquals(listOf(1.5f, 2f, 1f), seen)
        }
}
