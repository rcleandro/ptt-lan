package com.pttlan.feature.history

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.common.share.FileSharer
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.HistoryRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/** Sharing a message as a `.wav` (28.8). */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryShareTest {
    private val historyRepository: HistoryRepository = mockk(relaxed = true)
    private val fileSharer: FileSharer = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private val message = VoiceMessage("b", "Geral", "Ana", "/b.pcm", 1_000, 20)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { historyRepository.getAllMessages() } returns emptyFlow()
        every { fileSharer.shareDirectory } returns "/shared"
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createComponent() =
        HistoryComponent(DefaultComponentContext(LifecycleRegistry()), historyRepository, fileSharer, onBackClicked = {})

    @Test
    fun `sharing a message exports it as a wav and opens the share sheet with it`() =
        runTest(testDispatcher) {
            coEvery { historyRepository.exportAsWav(message, "/shared") } returns "/shared/b.wav"

            createComponent().shareMessage(message)
            advanceUntilIdle()

            verify(exactly = 1) { fileSharer.share("/shared/b.wav", "audio/wav") }
        }

    @Test
    fun `nothing is shared when the export fails`() =
        runTest(testDispatcher) {
            coEvery { historyRepository.exportAsWav(any(), any()) } returns null

            createComponent().shareMessage(message)
            advanceUntilIdle()

            verify(exactly = 0) { fileSharer.share(any(), any()) }
        }
}
