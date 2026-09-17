package com.pttlan.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.PttTheme

private const val HIGHLIGHT_FILL_ALPHA = 0.22f

/**
 * Round avatar with the name below. Blue ring = someone else speaking,
 * amber ring = the local user transmitting or asking for the floor.
 */
@Composable
fun ParticipantAvatar(
    name: String,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    isRequesting: Boolean = false,
    isSelf: Boolean = false,
    showDetails: Boolean = true,
) {
    val colors = PttTheme.customColors
    val highlight =
        when {
            isSpeaking && isSelf -> colors.accentTx
            isSpeaking -> MaterialTheme.colorScheme.primary
            isRequesting -> colors.accentTx
            else -> null
        }
    val ringColor by animateColorAsState(highlight ?: MaterialTheme.colorScheme.outline)
    val fillColor by animateColorAsState(highlight?.copy(alpha = HIGHLIGHT_FILL_ALPHA) ?: colors.surface3)

    Column(
        modifier = modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .drawBehind {
                        if (highlight != null) {
                            drawCircle(fillColor, radius = size.minDimension / 2 + 4.dp.toPx())
                        }
                    }.clip(CircleShape)
                    .background(fillColor)
                    .border(1.dp, ringColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        if (showDetails) {
            Text(
                text = if (isSelf) "Você" else name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (highlight != null) FontWeight.SemiBold else null,
                color = highlight ?: MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
