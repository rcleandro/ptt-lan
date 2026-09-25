package com.pttlan.feature.history

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.HistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.StandardTestDispatcher
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

            component.playMessage(mockMessage)

            // Since playMessage launches a coroutine, we can assert state if we step through,
            // but for now, we just advance to the end.
            advanceUntilIdle()

            coVerify(exactly = 1) { historyRepository.playMessage(mockMessage) }
            assertEquals(null, component.playingMessageId.value)
        }

    @Test
    fun `stopPlaying stops playback and clears playing state`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.stopPlaying()
            advanceUntilIdle()

            coVerify(exactly = 1) { historyRepository.stopPlayingMessage() }
            assertEquals(null, component.playingMessageId.value)
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
            component.playMessage(mockk(relaxed = true))
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

            component.playChannel("Geral")
            advanceUntilIdle()

            assertEquals(listOf("a", "b", "c"), played)
            assertEquals(null, component.playingMessageId.value)
            assertEquals(emptyList(), component.queue.value)
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

            component.playChannel("Geral")
            advanceUntilIdle()
            assertEquals("a", component.playingMessageId.value)
            assertEquals(listOf("a", "b", "c"), component.queue.value.map { it.id })

            ending.complete(Unit)
            advanceUntilIdle()
            assertEquals(null, component.playingMessageId.value)
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
            component.playChannel("Geral")
            advanceUntilIdle()

            holding = false
            component.playMessage(room[0])
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
            component.playChannel("Geral")
            advanceUntilIdle()

            holding = false
            component.playMessage(room[1])
            advanceUntilIdle()

            assertEquals(listOf("a", "x"), played)
            assertEquals(emptyList(), component.queue.value)
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
            component.playChannel("Geral")
            advanceUntilIdle()

            component.stopPlaying()
            advanceUntilIdle()

            assertEquals(listOf("a"), played)
            assertEquals(null, component.playingMessageId.value)
            assertEquals(emptyList(), component.queue.value)
        }
}
