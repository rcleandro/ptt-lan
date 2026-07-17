package com.pttlan.feature.history

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.VoiceRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryComponentTest {
    private val lifecycle = LifecycleRegistry()
    private val componentContext: ComponentContext = DefaultComponentContext(lifecycle)

    private val voiceRepository: VoiceRepository = mockk(relaxed = true)
    private val onBackClicked: () -> Unit = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        coEvery { voiceRepository.getAllMessages() } returns emptyFlow()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createComponent() =
        HistoryComponent(
            componentContext = componentContext,
            voiceRepository = voiceRepository,
            onBackClicked = onBackClicked,
        )

    @Test
    fun `initialization loads recent messages`() =
        runTest(testDispatcher) {
            val mockMessage = mockk<VoiceMessage>(relaxed = true)
            coEvery { mockMessage.id } returns "msg-1"
            coEvery { voiceRepository.getAllMessages() } returns flowOf(listOf(mockMessage))

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

            coVerify(exactly = 1) { voiceRepository.playMessage(mockMessage) }
            assertEquals(null, component.playingMessageId.value)
        }

    @Test
    fun `stopPlaying stops playback and clears playing state`() =
        runTest(testDispatcher) {
            val component = createComponent()

            component.stopPlaying()
            advanceUntilIdle()

            coVerify(exactly = 1) { voiceRepository.stopPlayingMessage() }
            assertEquals(null, component.playingMessageId.value)
        }
}
