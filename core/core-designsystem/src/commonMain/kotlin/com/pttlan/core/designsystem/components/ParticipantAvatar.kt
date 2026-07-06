package com.pttlan.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pttlan.core.designsystem.theme.PttTheme

@Composable
fun ParticipantAvatar(
    name: String,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true,
) {
    val initial = name.firstOrNull()?.uppercase() ?: "?"

    val borderColor by animateColorAsState(
        targetValue = if (isSpeaking) PttTheme.customColors.accentTx else MaterialTheme.colorScheme.outline,
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSpeaking) 2.dp else 1.dp,
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .background(PttTheme.customColors.surface3, CircleShape)
                    .border(borderWidth, borderColor, CircleShape)
                    .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (showDetails) {
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (isSpeaking) "falando" else "ouvindo",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = PttTheme.customColors.textTertiary,
                )
            }
        }
    }
}
