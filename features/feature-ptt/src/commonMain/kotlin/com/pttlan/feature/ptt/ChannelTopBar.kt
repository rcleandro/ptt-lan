package com.pttlan.feature.ptt

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.ConnectionStatus
import com.pttlan.core.designsystem.components.ConnectionStatusBadge
import com.pttlan.core.designsystem.components.GlassIconButton
import com.pttlan.core.designsystem.components.PttTopBar
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.glass
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.channel_name
import com.pttlan.core.designsystem.generated.resources.common_history
import com.pttlan.core.designsystem.generated.resources.ptt_back_to_channels
import com.pttlan.core.designsystem.generated.resources.ptt_participants
import org.jetbrains.compose.resources.stringResource

private val TitlePaddingHorizontal = 22.dp
private val TitlePaddingVertical = 5.dp

@Composable
internal fun ChannelTopBar(
    state: PttState,
    onIntent: (PttIntent) -> Unit,
    onBack: () -> Unit,
    showHistory: Boolean,
    connectionStatus: ConnectionStatus,
) {
    PttTopBar(
        navigation = { GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.ptt_back_to_channels), onBack) },
        center = { ChannelTitle(channelId = state.channelId, participantCount = state.participants.size) },
        actions = {
            // Only while the connection is not healthy: the channel title owns the center slot
            if (connectionStatus != ConnectionStatus.Online) {
                ConnectionStatusBadge(status = connectionStatus)
            }
            if (showHistory) {
                GlassIconButton(Icons.Default.History, stringResource(Res.string.common_history), { onIntent(PttIntent.GoToHistory) })
            }
        },
    )
}

@Composable
private fun ChannelTitle(
    channelId: String,
    participantCount: Int,
) {
    Column(
        modifier = Modifier.glass(CircleShape).padding(horizontal = TitlePaddingHorizontal, vertical = TitlePaddingVertical),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.channel_name, channelId),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SectionLabel(text = stringResource(Res.string.ptt_participants, participantCount))
    }
}
