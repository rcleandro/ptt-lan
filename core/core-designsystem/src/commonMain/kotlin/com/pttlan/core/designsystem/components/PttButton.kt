package com.pttlan.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.PttTheme

enum class PttButtonState {
    Idle,
    Requesting,
    Transmitting,
    Receiving,
}

@Composable
fun PttButton(
    state: PttButtonState,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 120.dp,
    buttonMargin: Dp = 16.dp,
) {
    val bgColor by animateColorAsState(
        targetValue =
            when (state) {
                PttButtonState.Idle -> PttTheme.customColors.surface3
                PttButtonState.Requesting -> PttTheme.customColors.accentTx
                PttButtonState.Transmitting -> PttTheme.customColors.statusTransmitting
                PttButtonState.Receiving -> MaterialTheme.colorScheme.primary
            },
    )

    val shadowColor by animateColorAsState(
        targetValue =
            when (state) {
                PttButtonState.Idle -> Color.Transparent
                PttButtonState.Requesting -> PttTheme.customColors.accentTxGlow
                PttButtonState.Transmitting -> PttTheme.customColors.statusTransmittingGlow
                PttButtonState.Receiving -> PttTheme.customColors.primaryGlow
            },
    )

    val iconColor by animateColorAsState(
        targetValue =
            when (state) {
                PttButtonState.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
                PttButtonState.Requesting, PttButtonState.Transmitting -> PttTheme.customColors.iconOnAccentTx
                PttButtonState.Receiving -> PttTheme.customColors.iconOnPrimary
            },
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(buttonSize + buttonMargin)
                .pointerInput(Unit) {
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
                    .shadow(
                        elevation = 8.dp,
                        shape = CircleShape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor,
                    ).background(bgColor, CircleShape)
                    .border(
                        width = 1.dp,
                        color =
                            if (state ==
                                PttButtonState.Idle
                            ) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                Color.Transparent
                            },
                        shape = CircleShape,
                    ).clip(CircleShape),
        ) {
            Icon(
                imageVector = if (state == PttButtonState.Receiving) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Mic,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(buttonSize * 0.35f),
            )
        }
    }
}
