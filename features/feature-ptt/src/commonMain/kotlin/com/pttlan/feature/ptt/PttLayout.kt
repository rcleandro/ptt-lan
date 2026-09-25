package com.pttlan.feature.ptt

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.ExpandedWidth

private val CompactHeight = 600.dp

/** Talk area and channel area side by side: landscape phones, Automotive head units, open Fold, tablet, Desktop. */
internal fun isSideBySide(
    width: Dp,
    height: Dp,
): Boolean = width >= ExpandedWidth || (width > height && height < CompactHeight)
