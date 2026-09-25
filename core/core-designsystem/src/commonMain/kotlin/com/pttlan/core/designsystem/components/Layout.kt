package com.pttlan.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val HALO_ALPHA = 0.22f

/** Widest a list, form or dock gets before it stops stretching (open Fold, tablet, Desktop). */
val ReadableWidth: Dp = 600.dp

/** From this width on (open Fold, tablet, Desktop) there is room for two panes side by side. */
val ExpandedWidth: Dp = 840.dp

/** Fills the width up to [ReadableWidth] and centers the content in the rest; no-op on phones. */
fun Modifier.readableWidth(): Modifier = fillMaxWidth().wrapContentWidth().widthIn(max = ReadableWidth).fillMaxWidth()

/** Floating top bar: glass controls over the content, no background of its own. */
@Composable
fun PttTopBar(
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = {},
    center: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { navigation() }
        center()
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

/** Small mono uppercase label for sections and eyebrows. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.3.sp),
        color = color,
    )
}

/** 8dp status dot; [halo] adds a soft ring, [hollow] draws only the outline (e.g. reconnecting). */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    halo: Boolean = false,
    hollow: Boolean = false,
) {
    Box(
        modifier =
            modifier
                .size(16.dp)
                .drawBehind {
                    if (halo) drawCircle(color.copy(alpha = HALO_ALPHA))
                    if (hollow) {
                        drawCircle(color, radius = 3.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
                    } else {
                        drawCircle(color, radius = 4.dp.toPx())
                    }
                },
    )
}
