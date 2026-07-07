package com.pttlan.feature.ptt

import app.cash.turbine.test
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.domain.ptt.repository.VoiceRepository
import com.pttlan.domain.ptt.usecase.JoinChannelUseCase
import com.pttlan.domain.ptt.usecase.LeaveChannelUseCase
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
    private val settings: Settings = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        every { settings.getString(any(), any()) } returns "TestUser"
        
        startKoin {
            modules(
                module {
                    single<Settings> { settings }
                }
            )
        }
        
        coEvery { observeParticipantsUseCase(any()) } returns emptyFlow()
        coEvery { observeSpeakerUseCase(any()) } returns emptyFlow()
        coEvery { observeFloorDeniedUseCase(any()) } returns emptyFlow()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createComponent() = PttComponent(
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
        stopTransmittingUseCase = stopTransmittingUseCase
    )

    @Test
    fun `initialization joins channel`() = runTest(testDispatcher) {
        val component = createComponent()
        testScheduler.advanceUntilIdle()
        
        coVerify(exactly = 1) { joinChannelUseCase("ch-1", "user-1", "TestUser") }
        assertEquals("ch-1", component.state.value.channelId)
    }

    @Test
    fun `PressPtt starts transmitting if floor is not blocked`() = runTest(testDispatcher) {
        val component = createComponent()
        testScheduler.advanceUntilIdle()
        
        component.onIntent(PttIntent.PressPtt)
        testScheduler.advanceUntilIdle()
        
        assertTrue(component.state.value.isTransmitting)
        coVerify(exactly = 1) { startTransmittingUseCase("ch-1", "user-1") }
    }

    @Test
    fun `ReleasePtt stops transmitting`() = runTest(testDispatcher) {
        val component = createComponent()
        testScheduler.advanceUntilIdle()
        
        component.onIntent(PttIntent.ReleasePtt)
        testScheduler.advanceUntilIdle()
        
        assertEquals(false, component.state.value.isTransmitting)
        coVerify(exactly = 1) { stopTransmittingUseCase("ch-1", "user-1") }
    }

    @Test
    fun `LeaveChannel intent leaves channel and navigates back`() = runTest(testDispatcher) {
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
    fun `observeFloorDeniedUseCase emits ShowFloorDenied effect`() = runTest(testDispatcher) {
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
}
