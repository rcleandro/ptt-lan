package com.pttlan.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.PttTheme

enum class ConnectionStatus {
    Online,
    Reconnecting,
    Offline,
}

@Composable
fun ConnectionStatusBadge(
    status: ConnectionStatus,
    modifier: Modifier = Modifier,
) {
    val dotColor by animateColorAsState(
        targetValue =
            when (status) {
                ConnectionStatus.Online -> PttTheme.customColors.statusOnline
                ConnectionStatus.Reconnecting -> PttTheme.customColors.statusIdle
                ConnectionStatus.Offline -> PttTheme.customColors.statusOffline
            },
    )

    val shadowColor by animateColorAsState(
        targetValue =
            if (status == ConnectionStatus.Online) {
                PttTheme.customColors.statusOnlineGlow
            } else {
                Color.Transparent
            },
    )

    Row(
        modifier =
            modifier
                .background(PttTheme.customColors.surface2, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .shadow(
                        elevation = if (status == ConnectionStatus.Online) 3.dp else 0.dp,
                        shape = CircleShape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor,
                    ).background(dotColor, CircleShape),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text =
                when (status) {
                    ConnectionStatus.Online -> "conectado"
                    ConnectionStatus.Reconnecting -> "reconectando"
                    ConnectionStatus.Offline -> "offline"
                },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
