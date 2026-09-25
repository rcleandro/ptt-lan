package com.pttlan.android

import android.graphics.Rect
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class TabletopFoldTest {
    // A Flip-sized window, 1080 x 2640 px, folding across its middle
    private val window = Rect(0, 0, 1080, 2640)

    private fun fold(
        state: FoldingFeature.State,
        orientation: FoldingFeature.Orientation,
    ) = TestWindowLayoutInfo(listOf(FoldingFeature(window, center = 1320, size = 0, state = state, orientation = orientation)))

    @Test
    fun `half open with the hinge across the screen is tabletop`() {
        val info = fold(FoldingFeature.State.HALF_OPENED, FoldingFeature.Orientation.HORIZONTAL)

        assertEquals(440.dp, tabletopFold(info, density = 3f))
    }

    @Test
    fun `flat, held like a book or without a hinge is not`() {
        assertNull(tabletopFold(fold(FoldingFeature.State.FLAT, FoldingFeature.Orientation.HORIZONTAL), density = 3f))
        assertNull(tabletopFold(fold(FoldingFeature.State.HALF_OPENED, FoldingFeature.Orientation.VERTICAL), density = 3f))
        assertNull(tabletopFold(TestWindowLayoutInfo(emptyList()), density = 3f))
    }
}
