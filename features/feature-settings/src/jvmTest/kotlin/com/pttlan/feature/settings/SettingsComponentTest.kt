package com.pttlan.feature.settings

import app.cash.turbine.test
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.russhwolf.settings.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsComponentTest {

    private val lifecycle = LifecycleRegistry()
    private val componentContext: ComponentContext = DefaultComponentContext(lifecycle)
    
    private val settings: Settings = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        every { settings.getString("nickname", "") } returns "TestUser"
        every { settings.getBoolean("use_opus", false) } returns false
        every { settings.getBoolean("use_dark_theme", true) } returns true
        every { settings.getBoolean("always_listening", true) } returns true
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createComponent() = SettingsComponent(
        componentContext = componentContext,
        settings = settings
    )

    @Test
    fun `initialization loads settings`() = runTest(testDispatcher) {
        val component = createComponent()
        
        assertEquals("TestUser", component.state.value.nickname)
        assertEquals(false, component.state.value.useOpus)
        assertEquals(true, component.state.value.useDarkTheme)
        assertEquals(true, component.state.value.alwaysListening)
    }

    @Test
    fun `UpdateNickname intent updates state and settings`() = runTest(testDispatcher) {
        val component = createComponent()
        
        component.onIntent(SettingsIntent.UpdateNickname("NewName"))
        
        assertEquals("NewName", component.state.value.nickname)
        verify(exactly = 1) { settings.putString("nickname", "NewName") }
    }

    @Test
    fun `ToggleOpus intent updates state and settings`() = runTest(testDispatcher) {
        val component = createComponent()
        
        component.onIntent(SettingsIntent.ToggleOpus(true))
        
        assertEquals(true, component.state.value.useOpus)
        verify(exactly = 1) { settings.putBoolean("use_opus", true) }
    }

    @Test
    fun `ToggleTheme intent updates state and settings`() = runTest(testDispatcher) {
        val component = createComponent()
        
        component.onIntent(SettingsIntent.ToggleTheme(false))
        
        assertEquals(false, component.state.value.useDarkTheme)
        verify(exactly = 1) { settings.putBoolean("use_dark_theme", false) }
    }

    @Test
    fun `ToggleAlwaysListening intent updates state and settings`() = runTest(testDispatcher) {
        val component = createComponent()
        
        component.onIntent(SettingsIntent.ToggleAlwaysListening(false))
        
        assertEquals(false, component.state.value.alwaysListening)
        verify(exactly = 1) { settings.putBoolean("always_listening", false) }
    }
}
