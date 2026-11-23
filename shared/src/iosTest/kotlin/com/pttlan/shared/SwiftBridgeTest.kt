package com.pttlan.shared

import com.pttlan.core.designsystem.theme.AppTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SwiftBridgeTest {
    @Test
    fun observingDeliversEveryValueInOrderUntilCancelled() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        val flow = MutableStateFlow(1)
        val seen = mutableListOf<Any?>()

        val observation = observe(flow, dispatcher) { seen += it }
        scope.runCurrent()
        flow.value = 2
        scope.runCurrent()
        observation.cancel()
        flow.value = 3
        scope.runCurrent()

        assertEquals(listOf<Any?>(1, 2), seen, "a SwiftUI screen that went away must stop receiving")
    }

    @Test
    fun theThemeChosenInSettingsWinsOverTheSystemOne() {
        assertTrue(isDarkTheme(AppTheme.SYSTEM, systemIsDark = true))
        assertFalse(isDarkTheme(AppTheme.SYSTEM, systemIsDark = false))
        assertTrue(isDarkTheme(AppTheme.DARK, systemIsDark = false))
        assertFalse(isDarkTheme(AppTheme.LIGHT, systemIsDark = true))
    }

    @Test
    fun eachThemeHasItsOwnPalette() {
        assertTrue(swiftPalette(dark = true).background != swiftPalette(dark = false).background)
    }
}
