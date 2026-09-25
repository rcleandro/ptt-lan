package com.pttlan.core.designsystem.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Folding or unfolding mid-press recreates the screen and the finger never lifts from the button (27.5). The
 * press must still end, or the microphone stays on and the floor stays taken for everyone else.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PttButtonReleaseTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `a press cut off by the screen going away still ends it`() {
        var shown by mutableStateOf(true)
        var starts = 0
        var ends = 0
        composeRule.setContent {
            if (shown) PttButton(state = PttButtonState.Idle, onPressStart = { starts++ }, onPressEnd = { ends++ })
        }

        composeRule.onRoot().performTouchInput { down(center) }
        composeRule.waitForIdle()
        shown = false
        composeRule.waitForIdle()

        assertEquals(1, starts)
        assertEquals(1, ends)
    }
}
