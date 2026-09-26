package com.pttlan.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme

private const val GLOW_FADE_STOP = 0.66f

/**
 * Liquid Glass material (ADR 0006) for the floating control layer: bars, docks, pills and round buttons.
 * Content (lists, cards) must use [contentCard] instead — never glass on glass.
 */
@Composable
fun Modifier.glass(shape: Shape): Modifier {
    val colors = PttTheme.customColors
    if (PttTheme.reduceTransparency) {
        return clip(shape)
            .background(colors.surface2, shape)
            .border(Dimens.Hairline, MaterialTheme.colorScheme.outline, shape)
    }
    // ponytail: translucent layers without backdrop blur; real blur needs Haze on Compose MP 1.12+ (ADR 0006)
    return clip(shape)
        .background(colors.glassBase, shape)
        .background(Brush.verticalGradient(listOf(colors.glassTop, colors.glassBottom)), shape)
        .border(Dimens.Hairline, Brush.verticalGradient(listOf(colors.glassHighlight, colors.glassStroke)), shape)
}

/** Calm, solid surface for content that scrolls under the glass layer. */
@Composable
fun Modifier.contentCard(shape: Shape = MaterialTheme.shapes.medium): Modifier =
    clip(shape)
        .background(MaterialTheme.colorScheme.surface, shape)
        .border(Dimens.Hairline, MaterialTheme.colorScheme.outline, shape)

/**
 * Soft ambient light behind the glass. Its color carries state (free, transmitting, receiving),
 * so it is never used as decoration alone.
 */
@Composable
fun AmbientGlow(
    color: Color,
    modifier: Modifier = Modifier,
    intensity: Float = 0.3f,
) {
    val reduced = PttTheme.reduceTransparency
    Box(
        modifier =
            modifier.drawBehind {
                if (reduced) return@drawBehind
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            0f to color.copy(alpha = intensity),
                            GLOW_FADE_STOP to Color.Transparent,
                            center = center,
                            radius = size.minDimension / 2,
                        ),
                )
            },
    )
}
