package com.pttlan.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.pttlan.core.designsystem.components.PttButtonState
import com.pttlan.core.designsystem.components.screenReaderActionLabel
import com.pttlan.core.designsystem.components.screenReaderStartsTalking
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.ptt_action_start
import com.pttlan.core.designsystem.generated.resources.ptt_action_stop
import com.pttlan.core.designsystem.theme.BgDark
import com.pttlan.core.designsystem.theme.BgLight
import com.pttlan.core.designsystem.theme.Surface3Dark
import com.pttlan.core.designsystem.theme.Surface3Light
import com.pttlan.core.designsystem.theme.TextSecondaryDark
import com.pttlan.core.designsystem.theme.TextSecondaryLight
import com.pttlan.core.designsystem.theme.TextTertiaryDark
import com.pttlan.core.designsystem.theme.TextTertiaryLight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val WCAG_AA_SMALL_TEXT = 4.5f
private const val LUMINANCE_OFFSET = 0.05f

class ScreenReaderTest {
    @Test
    fun theDoubleTapStartsWhenNotTalkingAndStopsWhenTalkingOrAsking() {
        assertTrue(PttButtonState.Idle.screenReaderStartsTalking())
        assertTrue(PttButtonState.Receiving.screenReaderStartsTalking())
        assertFalse(PttButtonState.Requesting.screenReaderStartsTalking())
        assertFalse(PttButtonState.Transmitting.screenReaderStartsTalking())
        assertEquals(Res.string.ptt_action_start, PttButtonState.Idle.screenReaderActionLabel())
        assertEquals(Res.string.ptt_action_stop, PttButtonState.Transmitting.screenReaderActionLabel())
    }

    @Test
    fun secondaryTextsReachWcagAaOnEveryBackground() {
        listOf(
            TextSecondaryDark to BgDark,
            TextSecondaryDark to Surface3Dark,
            TextTertiaryDark to BgDark,
            TextTertiaryDark to Surface3Dark,
            TextSecondaryLight to BgLight,
            TextSecondaryLight to Surface3Light,
            TextTertiaryLight to BgLight,
            TextTertiaryLight to Surface3Light,
        ).forEach { (text, background) ->
            val ratio = contrast(text, background)
            assertTrue(ratio >= WCAG_AA_SMALL_TEXT, "$text on $background: $ratio")
        }
    }

    private fun contrast(
        a: Color,
        b: Color,
    ): Float {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + LUMINANCE_OFFSET) / (dark + LUMINANCE_OFFSET)
    }
}
