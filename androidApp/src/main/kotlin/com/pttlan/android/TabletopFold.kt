package com.pttlan.android

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo

/**
 * Where a half-open foldable lying flat bends, from the top of the window: a Flip or a Fold half open on a table
 * with the hinge across the screen. Null when flat, closed, or held like a book (hinge running top to bottom).
 */
internal fun tabletopFold(
    info: WindowLayoutInfo,
    density: Float,
): Dp? =
    info.displayFeatures
        .filterIsInstance<FoldingFeature>()
        .firstOrNull { it.state == FoldingFeature.State.HALF_OPENED && it.orientation == FoldingFeature.Orientation.HORIZONTAL }
        ?.let { (it.bounds.top / density).dp }
