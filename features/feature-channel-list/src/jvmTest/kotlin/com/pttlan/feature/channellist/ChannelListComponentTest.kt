package com.pttlan.feature.channellist

import app.cash.turbine.test
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.domain.ptt.model.VoiceMessage
import com.pttlan.domain.ptt.repository.ActiveChannelDomain
import com.pttlan.domain.ptt.repository.ChannelDomain
import com.pttlan.domain.ptt.usecase.CreateChannelUseCase
import com.pttlan.domain.ptt.usecase.GetRecentChannelsUseCase
import com.pttlan.domain.ptt.usecase.JoinChannelUseCaseImpl
import com.pttlan.domain.ptt.usecase.ObserveActiveChannelsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
class ChannelListComponentTest {

    private val lifecycle = LifecycleRegistry()
    private val componentContext: ComponentContext = DefaultComponentContext(lifecycle)
    
    private val getRecentChannelsUseCase: GetRecentChannelsUseCase = mockk(relaxed = true)
    private val observeActiveChannelsUseCase: ObserveActiveChannelsUseCase = mockk(relaxed = true)
    private val joinChannelUseCase: JoinChannelUseCaseImpl = mockk(relaxed = true)
    private val createChannelUseCase: CreateChannelUseCase = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        coEvery { getRecentChannelsUseCase() } returns emptyFlow()
        coEvery { observeActiveChannelsUseCase() } returns emptyFlow()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createComponent() = ChannelListComponent(
        componentContext = componentContext,
        getRecentChannelsUseCase = getRecentChannelsUseCase,
        observeActiveChannelsUseCase = observeActiveChannelsUseCase,
        joinChannelUseCase = joinChannelUseCase,
        createChannelUseCase = createChannelUseCase
    )

    @Test
    fun `initialization loads recent and active channels`() = runTest(testDispatcher) {
        val recentChannel = ChannelDomain("ch-1", "Channel 1", false)
        val activeChannel = ActiveChannelDomain("ch-2", 5)
        
        coEvery { getRecentChannelsUseCase() } returns flowOf(listOf(recentChannel))
        coEvery { observeActiveChannelsUseCase() } returns flowOf(listOf(activeChannel))
        
        val component = createComponent()
        advanceUntilIdle()
        
        assertEquals(listOf(recentChannel), component.state.value.recentChannels)
        assertEquals(listOf(activeChannel), component.state.value.activeChannels)
    }

    @Test
    fun `UpdateNewChannelName intent updates state`() = runTest(testDispatcher) {
        val component = createComponent()
        
        component.onIntent(ChannelListIntent.UpdateNewChannelName("New Channel"))
        advanceUntilIdle()
        
        assertEquals("New Channel", component.state.value.newChannelName)
    }

    @Test
    fun `JoinChannel intent calls use case and emits effect`() = runTest(testDispatcher) {
        val component = createComponent()
        advanceUntilIdle()
        
        component.effects.test {
            component.onIntent(ChannelListIntent.JoinChannel("ch-1", "User"))
            advanceUntilIdle()
            
            coVerify(exactly = 1) { joinChannelUseCase("ch-1", "User") }
            assertEquals(ChannelListEffect.NavigateToChannel("ch-1"), awaitItem())
        }
    }

    @Test
    fun `CreateChannel intent calls use case and emits effect if name is not blank`() = runTest(testDispatcher) {
        coEvery { createChannelUseCase("My Channel") } returns "new-ch-id"
        
        val component = createComponent()
        component.onIntent(ChannelListIntent.UpdateNewChannelName("My Channel"))
        advanceUntilIdle()
        
        component.effects.test {
            component.onIntent(ChannelListIntent.CreateChannel)
            advanceUntilIdle()
            
            coVerify(exactly = 1) { createChannelUseCase("My Channel") }
            assertEquals(ChannelListEffect.NavigateToChannel("new-ch-id"), awaitItem())
        }
    }

    @Test
    fun `CreateChannel intent does not call use case if name is blank`() = runTest(testDispatcher) {
        val component = createComponent()
        component.onIntent(ChannelListIntent.UpdateNewChannelName("   "))
        advanceUntilIdle()
        
        component.onIntent(ChannelListIntent.CreateChannel)
        advanceUntilIdle()
        
        coVerify(exactly = 0) { createChannelUseCase(any()) }
    }
}
