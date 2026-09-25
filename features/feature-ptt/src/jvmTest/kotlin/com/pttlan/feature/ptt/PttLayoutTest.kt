package com.pttlan.feature.ptt

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PttLayoutTest {
    @Test
    fun `open fold and tablets put talk and channel areas side by side`() {
        assertTrue(isSideBySide(width = 880.dp, height = 1000.dp))
        assertTrue(isSideBySide(width = 1280.dp, height = 800.dp))
    }

    @Test
    fun `landscape phones stay side by side`() {
        assertTrue(isSideBySide(width = 850.dp, height = 400.dp))
    }

    @Test
    fun `portrait phones, closed fold and flip cover screen stack the areas`() {
        assertFalse(isSideBySide(width = 400.dp, height = 850.dp))
        assertFalse(isSideBySide(width = 360.dp, height = 374.dp))
        assertFalse(isSideBySide(width = 700.dp, height = 900.dp))
    }
}
