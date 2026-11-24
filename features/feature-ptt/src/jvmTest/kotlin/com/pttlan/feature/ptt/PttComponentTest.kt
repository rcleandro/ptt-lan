package com.pttlan.feature.ptt

import app.cash.turbine.test
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.pttlan.domain.ptt.model.ParticipantDomain
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.SpeakerState
import com.pttlan.domain.ptt.repository.VoiceRepository
import com.pttlan.domain.ptt.usecase.JoinChannelUseCase
import com.pttlan.domain.ptt.usecase.LeaveChannelUseCase
import com.pttlan.domain.ptt.usecase.ObserveConnectionStatusUseCase
import com.pttlan.domain.ptt.usecase.ObserveFloorDeniedUseCase
import com.pttlan.domain.ptt.usecase.ObserveParticipantsUseCase
import com.pttlan.domain.ptt.usecase.ObserveSpeakerUseCase
import com.pttlan.domain.ptt.usecase.StartTransmittingUseCase
import com.pttlan.domain.ptt.usecase.StopTransmittingUseCase
import com.russhwolf.settings.Settings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PttComponentTest {
    private val lifecycle = LifecycleRegistry()
    private val componentContext: ComponentContext = DefaultComponentContext(lifecycle)

    private val voiceRepository: VoiceRepository = mockk(relaxed = true)
    private val joinChannelUseCase: JoinChannelUseCase = mockk(relaxed = true)
    private val leaveChannelUseCase: LeaveChannelUseCase = mockk(relaxed = true)
    private val observeParticipantsUseCase: ObserveParticipantsUseCase = mockk(relaxed = true)
    private val observeSpeakerUseCase: ObserveSpeakerUseCase = mockk(relaxed = true)
    private val observeFloorDeniedUseCase: ObserveFloorDeniedUseCase = mockk(relaxed = true)
    private val startTransmittingUseCase: StartTransmittingUseCase = mockk(relaxed = true)
    private val stopTransmittingUseCase: StopTransmittingUseCase = mockk(relaxed = true)
    private val observeConnectionStatusUseCase: ObserveConnectionStatusUseCase = mockk(relaxed = true)
    private val settings: Settings = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { settings.getStringOrNull(any()) } returns "TestUser"

        startKoin {
            modules(
                module {
                    single<Settings> { settings }
                },
            )
        }

        coEvery { observeParticipantsUseCase(any()) } returns emptyFlow()
        coEvery { observeSpeakerUseCase(any()) } returns emptyFlow()
        coEvery { observeFloorDeniedUseCase(any()) } returns emptyFlow()
        every { observeConnectionStatusUseCase() } returns emptyFlow()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createComponent() =
        PttComponent(
            componentContext = componentContext,
            channelId = "ch-1",
            userId = "user-1",
            voiceRepository = voiceRepository,
            joinChannelUseCase = joinChannelUseCase,
            leaveChannelUseCase = leaveChannelUseCase,
            observeParticipantsUseCase = observeParticipantsUseCase,
            observeSpeakerUseCase = observeSpeakerUseCase,
            observeFloorDeniedUseCase = observeFloorDeniedUseCase,
            startTransmittingUseCase = startTransmittingUseCase,
            stopTransmittingUseCase = stopTransmittingUseCase,
            observeConnectionStatusUseCase = observeConnectionStatusUseCase,
        )

    @Test
    fun `initialization joins channel`() =
        runTest(testDispatcher) {
            val component = createComponent()
            testScheduler.advanceUntilIdle()

            coVerify(exactly = 1) { joinChannelUseCase("ch-1", "user-1", "TestUser") }
            assertEquals("ch-1", component.state.value.channelId)
        }

    @Test
    fun `PressPtt starts transmitting if floor is not blocked`() =
        runTest(testDispatcher) {
            val component = createComponent()
            testScheduler.advanceUntilIdle()

            component.onIntent(PttIntent.PressPtt)
            testScheduler.advanceUntilIdle()

            assertTrue(component.state.value.isTransmitting)
            coVerify(exactly = 1) { startTransmittingUseCase("ch-1", "user-1") }
        }

    @Test
    fun `leaving the screen mid-press stops transmitting`() =
        runTest(testDispatcher) {
            // Back while holding the PTT key: the release lands on another screen and was dropped, and the
            // on-screen button's gesture is cancelled without a release, so the microphone stayed on.
            val component = createComponent()
            testScheduler.advanceUntilIdle()
            component.onIntent(PttIntent.PressPtt)
            testScheduler.advanceUntilIdle()

            lifecycle.resume()
            lifecycle.destroy()

            coVerify(timeout = 2_000) { stopTransmittingUseCase("ch-1", "user-1") }
        }

    @Test
    fun `leaving the screen without transmitting does not touch the floor`() =
        runTest(testDispatcher) {
            createComponent()
            testScheduler.advanceUntilIdle()

            lifecycle.resume()
            lifecycle.destroy()

            coVerify(timeout = 2_000) { leaveChannelUseCase("ch-1", "user-1") }
            coVerify(exactly = 0) { stopTransmittingUseCase(any(), any()) }
        }

    @Test
    fun `ReleasePtt stops transmitting`() =
        runTest(testDispatcher) {
            val component = createComponent()
            testScheduler.advanceUntilIdle()

            component.onIntent(PttIntent.ReleasePtt)
            testScheduler.advanceUntilIdle()

            assertEquals(false, component.state.value.isTransmitting)
            coVerify(exactly = 1) { stopTransmittingUseCase("ch-1", "user-1") }
        }

    @Test
    fun `LeaveChannel intent leaves channel and navigates back`() =
        runTest(testDispatcher) {
            val component = createComponent()
            testScheduler.advanceUntilIdle()

            component.effects.test {
                component.onIntent(PttIntent.LeaveChannel)
                testScheduler.advanceUntilIdle()

                assertEquals(PttEffect.NavigateBack, awaitItem())
                coVerify(exactly = 1) { leaveChannelUseCase("ch-1", "user-1") }
            }
        }

    @Test
    fun `observeFloorDeniedUseCase emits ShowFloorDenied effect`() =
        runTest(testDispatcher) {
            val floorDeniedFlow = MutableSharedFlow<String>()
            coEvery { observeFloorDeniedUseCase("ch-1") } returns floorDeniedFlow

            val component = createComponent()
            testScheduler.advanceUntilIdle()

            component.effects.test {
                floorDeniedFlow.emit("Server Error")
                testScheduler.advanceUntilIdle()

                assertEquals(PttEffect.ShowFloorDenied("Server Error"), awaitItem())
                assertEquals(false, component.state.value.isTransmitting)
            }
        }

    /** Achado 1 da rodada da 29.1: entering mid-turn, the list already says who speaks before any SpeakerChanged. */
    @Test
    fun `joining while someone speaks reads the channel as busy from the participant list`() =
        runTest(testDispatcher) {
            val participants = MutableSharedFlow<List<ParticipantDomain>>()
            coEvery { observeParticipantsUseCase("ch-1") } returns participants
            val component = createComponent()
            testScheduler.advanceUntilIdle()

            participants.emit(listOf(ParticipantDomain("user-2", "load1", isSpeaking = true), ParticipantDomain("user-1", "Eu", false)))
            testScheduler.advanceUntilIdle()

            assertEquals("user-2", component.state.value.currentSpeakerId)
            assertEquals("load1", component.state.value.currentSpeakerName)
            assertTrue(component.state.value.floorBlocked)
        }

    @Test
    fun `the participant list does not undo a speaker change nor make the user receive their own turn`() =
        runTest(testDispatcher) {
            val participants = MutableSharedFlow<List<ParticipantDomain>>()
            val speaker = MutableSharedFlow<SpeakerState>()
            coEvery { observeParticipantsUseCase("ch-1") } returns participants
            coEvery { observeSpeakerUseCase("ch-1") } returns speaker
            val component = createComponent()
            testScheduler.advanceUntilIdle()

            participants.emit(listOf(ParticipantDomain("user-1", "Eu", isSpeaking = true)))
            testScheduler.advanceUntilIdle()
            assertEquals(null, component.state.value.currentSpeakerId)

            speaker.emit(SpeakerState("user-3", "Ana", true))
            participants.emit(listOf(ParticipantDomain("user-2", "load1", isSpeaking = true)))
            testScheduler.advanceUntilIdle()
            assertEquals("user-3", component.state.value.currentSpeakerId)
        }

    /** Achado 2: while reconnecting the screen must not read as a free channel, and pressing sends nothing. */
    @Test
    fun `while reconnecting the press is ignored`() =
        runTest(testDispatcher) {
            val status = MutableSharedFlow<ConnectionStatus>()
            every { observeConnectionStatusUseCase() } returns status
            val component = createComponent()
            testScheduler.advanceUntilIdle()

            status.emit(ConnectionStatus.Reconnecting)
            testScheduler.advanceUntilIdle()
            component.onIntent(PttIntent.PressPtt)
            testScheduler.advanceUntilIdle()

            assertEquals(false, component.state.value.isOnline)
            assertEquals(false, component.state.value.isTransmitting)
            coVerify(exactly = 0) { startTransmittingUseCase(any(), any()) }

            status.emit(ConnectionStatus.Connected)
            testScheduler.advanceUntilIdle()
            assertTrue(component.state.value.isOnline)
        }
}
