package com.pttlan.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.status_offline
import com.pttlan.core.designsystem.generated.resources.status_online
import com.pttlan.core.designsystem.generated.resources.status_reconnecting
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme
import org.jetbrains.compose.resources.stringResource

/** Glass status pill for the top bar. */
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

    Row(
        modifier =
            modifier
                .height(Dimens.GlassControl)
                .glass(CircleShape)
                .padding(start = Dimens.SpaceLg, end = Dimens.SpaceXl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        StatusDot(
            color = dotColor,
            halo = status == ConnectionStatus.Online,
            hollow = status == ConnectionStatus.Reconnecting,
        )
        Text(
            text =
                when (status) {
                    ConnectionStatus.Online -> stringResource(Res.string.status_online)
                    ConnectionStatus.Reconnecting -> stringResource(Res.string.status_reconnecting)
                    ConnectionStatus.Offline -> stringResource(Res.string.status_offline)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
