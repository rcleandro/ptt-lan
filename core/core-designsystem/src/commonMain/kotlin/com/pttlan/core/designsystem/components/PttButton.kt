package com.pttlan.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.ptt_button_idle
import com.pttlan.core.designsystem.generated.resources.ptt_button_receiving
import com.pttlan.core.designsystem.generated.resources.ptt_button_requesting
import com.pttlan.core.designsystem.generated.resources.ptt_button_transmitting
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme
import org.jetbrains.compose.resources.stringResource

private const val PRESSED_SCALE = 0.97f
private const val GLOW_RADIUS_FACTOR = 1.25f
private const val IDLE_GLOW_ALPHA = 0.18f
private const val TINT_GLOW_ALPHA = 0.5f
private const val RING_COUNT = 3
private const val RING_BASE_ALPHA = 0.4f
private const val TINT_HIGHLIGHT_MIX = 0.6f
private const val TINT_EDGE_DARKEN = 0.3f
private const val TINT_ALPHA = 0.9f
private const val BOTTOM_SHADE_ALPHA = 0.22f
private const val LENS_GRADIENT_SCALE = 1.2f
private const val SHEEN_ALPHA = 0.4f
private const val SHEEN_BOTTOM = 0.34f
private val DefaultButtonSize = 208.dp
private val RingSpacing = 16.dp
private val RequestRingGap = 10.dp
private val RequestRingStroke = 2.dp
private val RequestRingDash = 10.dp
private val RequestRingDashGap = 8.dp

/**
 * Push-to-talk lens (ADR 0006). Clear glass when the channel is free, amber-tinted while transmitting,
 * blue-tinted while someone else speaks, and a dashed amber ring while waiting for the floor.
 */
@Composable
fun PttButton(
    state: PttButtonState,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = DefaultButtonSize,
    buttonMargin: Dp = Dimens.SpaceXl,
) {
    val colors = PttTheme.customColors
    val primary = MaterialTheme.colorScheme.primary
    val isTinted = state == PttButtonState.Transmitting || state == PttButtonState.Receiving

    val tint by animateColorAsState(if (state == PttButtonState.Receiving) primary else colors.accentTx)
    val tintProgress by animateFloatAsState(if (isTinted) 1f else 0f)
    val requestProgress by animateFloatAsState(if (state == PttButtonState.Requesting) 1f else 0f)
    val scale by animateFloatAsState(
        if (state == PttButtonState.Transmitting || state == PttButtonState.Requesting) PRESSED_SCALE else 1f,
    )
    val iconColor by animateColorAsState(
        when (state) {
            PttButtonState.Idle -> MaterialTheme.colorScheme.onBackground
            PttButtonState.Requesting -> colors.accentTx
            PttButtonState.Transmitting -> colors.iconOnAccentTx
            PttButtonState.Receiving -> colors.iconOnPrimary
        },
    )
    val label =
        when (state) {
            PttButtonState.Idle -> stringResource(Res.string.ptt_button_idle)
            PttButtonState.Requesting -> stringResource(Res.string.ptt_button_requesting)
            PttButtonState.Transmitting -> stringResource(Res.string.ptt_button_transmitting)
            PttButtonState.Receiving -> stringResource(Res.string.ptt_button_receiving)
        }
    val lens =
        LensColors(
            idleHighlight = colors.glassHighlight,
            idleMid = colors.glassTop,
            idleEdge = colors.glassBottom,
            base = colors.glassBase,
            idleBorder = colors.glassStroke,
            idleGlow = primary,
            accent = colors.accentTx,
        )

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(buttonSize + buttonMargin)
                .drawBehind { drawHalo(buttonSize.toPx() / 2, tint, tintProgress, requestProgress, lens) }
                .semantics {
                    contentDescription = label
                    role = Role.Button
                }.pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onPressStart()
                            try {
                                awaitRelease()
                            } finally {
                                onPressEnd()
                            }
                        },
                    )
                },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(buttonSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }.drawBehind { drawLens(tint, tintProgress, lens) },
        ) {
            Icon(
                imageVector = if (state == PttButtonState.Receiving) Icons.Default.GraphicEq else Icons.Default.Mic,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(buttonSize * 0.3f),
            )
        }
    }
}

private data class LensColors(
    val idleHighlight: Color,
    val idleMid: Color,
    val idleEdge: Color,
    val base: Color,
    val idleBorder: Color,
    val idleGlow: Color,
    val accent: Color,
)

private fun DrawScope.drawHalo(
    radius: Float,
    tint: Color,
    tintProgress: Float,
    requestProgress: Float,
    lens: LensColors,
) {
    val glow =
        lerp(
            lens.idleGlow.copy(alpha = IDLE_GLOW_ALPHA),
            tint.copy(alpha = TINT_GLOW_ALPHA),
            tintProgress,
        )
    val glowRadius = radius * 2 * GLOW_RADIUS_FACTOR
    drawCircle(
        brush = Brush.radialGradient(listOf(glow, Color.Transparent), center = center, radius = glowRadius),
        radius = glowRadius,
    )

    if (tintProgress > 0f) {
        for (ring in 1..RING_COUNT) {
            drawCircle(
                color = tint.copy(alpha = RING_BASE_ALPHA / ring * tintProgress),
                radius = radius + RingSpacing.toPx() * ring,
                style = Stroke(width = Dimens.Hairline.toPx()),
            )
        }
    }

    if (requestProgress > 0f) {
        drawCircle(
            color = lens.accent.copy(alpha = requestProgress),
            radius = radius + RequestRingGap.toPx(),
            style =
                Stroke(
                    width = RequestRingStroke.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(RequestRingDash.toPx(), RequestRingDashGap.toPx())),
                ),
        )
    }
}

private fun DrawScope.drawLens(
    tint: Color,
    tintProgress: Float,
    lens: LensColors,
) {
    val tinted = tint.copy(alpha = TINT_ALPHA)
    val highlight = lerp(lens.idleHighlight, lerp(tint, Color.White, TINT_HIGHLIGHT_MIX), tintProgress)
    val mid = lerp(lens.idleMid, tinted, tintProgress)
    val edge = lerp(lens.idleEdge, lerp(tint, Color.Black, TINT_EDGE_DARKEN), tintProgress)

    drawCircle(lens.base)
    drawCircle(
        brush =
            Brush.radialGradient(
                0f to highlight,
                0.46f to mid,
                0.8f to edge,
                center = Offset(size.width * 0.3f, size.height * 0.18f),
                radius = size.maxDimension * LENS_GRADIENT_SCALE,
            ),
    )
    drawCircle(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = BOTTOM_SHADE_ALPHA)))
    drawOval(
        brush =
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = SHEEN_ALPHA), Color.Transparent),
                startY = size.height * 0.06f,
                endY = size.height * SHEEN_BOTTOM,
            ),
        topLeft = Offset(size.width * 0.19f, size.height * 0.06f),
        size = Size(size.width * 0.62f, size.height * 0.28f),
    )
    drawCircle(
        color = lerp(lens.idleBorder, lerp(tint, Color.White, TINT_HIGHLIGHT_MIX), tintProgress),
        radius = size.minDimension / 2 - Dimens.Hairline.toPx() / 2,
        style = Stroke(width = Dimens.Hairline.toPx()),
    )
}
