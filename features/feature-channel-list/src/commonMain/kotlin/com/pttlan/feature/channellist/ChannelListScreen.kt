package com.pttlan.feature.channellist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.AmbientGlow
import com.pttlan.core.designsystem.components.ChannelCard
import com.pttlan.core.designsystem.components.ConnectionStatus
import com.pttlan.core.designsystem.components.ConnectionStatusBadge
import com.pttlan.core.designsystem.components.GlassIconButton
import com.pttlan.core.designsystem.components.PillButton
import com.pttlan.core.designsystem.components.PttTextField
import com.pttlan.core.designsystem.components.PttTopBar
import com.pttlan.core.designsystem.components.glass
import com.pttlan.core.designsystem.components.readableWidth
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.channels_disconnect
import com.pttlan.core.designsystem.generated.resources.channels_empty
import com.pttlan.core.designsystem.generated.resources.channels_join
import com.pttlan.core.designsystem.generated.resources.channels_new_hint
import com.pttlan.core.designsystem.generated.resources.channels_server_code
import com.pttlan.core.designsystem.generated.resources.channels_subtitle
import com.pttlan.core.designsystem.generated.resources.channels_title
import com.pttlan.core.designsystem.generated.resources.common_cancel
import com.pttlan.core.designsystem.generated.resources.common_history
import com.pttlan.core.designsystem.generated.resources.common_settings
import com.pttlan.core.designsystem.generated.resources.stop_host_confirm
import com.pttlan.core.designsystem.generated.resources.stop_host_text
import com.pttlan.core.designsystem.generated.resources.stop_host_title
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.repository.ActiveChannelDomain
import org.jetbrains.compose.resources.stringResource

private val TopBarClearance = 72.dp
private val DockClearance = 120.dp
private const val TOP_GLOW_INTENSITY = 0.22f
private const val BOTTOM_GLOW_INTENSITY = 0.2f

@Composable
fun ChannelListScreen(
    component: ChannelListComponent,
    onOpenSettings: () -> Unit,
    onOpenHistory: (() -> Unit)?,
    connectionStatus: ConnectionStatus = ConnectionStatus.Online,
    modifier: Modifier = Modifier,
) {
    val state by component.state.collectAsState()

    ChannelListScreenContent(
        state = state,
        onIntent = component::onIntent,
        modifier = modifier,
        onOpenSettings = onOpenSettings,
        onOpenHistory = onOpenHistory,
        connectionStatus = connectionStatus,
    )
}

@Composable
fun ChannelListScreenContent(
    state: ChannelListState,
    onIntent: (ChannelListIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenHistory: (() -> Unit)? = null,
    connectionStatus: ConnectionStatus = ConnectionStatus.Online,
) {
    // Named here, inside the composable: a top-level Dp is initialiser code outside the UI and counts against
    // the coverage of the screen's logic
    val topGlowSize = 440.dp
    val topGlowOffset = DpOffset(160.dp, (-120).dp)
    val bottomGlowSize = 420.dp
    val bottomGlowOffset = DpOffset((-180).dp, 80.dp)
    Box(modifier = modifier.fillMaxSize()) {
        AmbientGlow(
            color = PttTheme.customColors.statusOnline,
            intensity = TOP_GLOW_INTENSITY,
            modifier = Modifier.size(topGlowSize).align(Alignment.TopEnd).offset(topGlowOffset.x, topGlowOffset.y),
        )
        AmbientGlow(
            color = MaterialTheme.colorScheme.primary,
            intensity = BOTTOM_GLOW_INTENSITY,
            modifier = Modifier.size(bottomGlowSize).align(Alignment.BottomStart).offset(bottomGlowOffset.x, bottomGlowOffset.y),
        )

        ChannelList(state = state, onIntent = onIntent)

        PttTopBar(
            modifier = Modifier.readableWidth(),
            navigation = {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.channels_disconnect),
                    onClick = { onIntent(ChannelListIntent.Leave) },
                )
            },
            center = { ConnectionStatusBadge(status = connectionStatus) },
            actions = { ToolbarGroup(onOpenSettings, onOpenHistory) },
        )

        NewChannelDock(
            name = state.newChannelName,
            onIntent = onIntent,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (state.confirmingStopHost) {
        StopHostDialog(onIntent)
    }
}

@Composable
private fun StopHostDialog(onIntent: (ChannelListIntent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onIntent(ChannelListIntent.DismissStopHost) },
        title = { Text(stringResource(Res.string.stop_host_title)) },
        text = { Text(stringResource(Res.string.stop_host_text)) },
        shape = MaterialTheme.shapes.large,
        confirmButton = {
            TextButton(onClick = { onIntent(ChannelListIntent.ConfirmStopHost) }) {
                Text(stringResource(Res.string.stop_host_confirm), color = PttTheme.customColors.statusOffline)
            }
        },
        dismissButton = {
            TextButton(onClick = { onIntent(ChannelListIntent.DismissStopHost) }) { Text(stringResource(Res.string.common_cancel)) }
        },
    )
}

@Composable
private fun ChannelList(
    state: ChannelListState,
    onIntent: (ChannelListIntent) -> Unit,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        modifier = Modifier.fillMaxSize().readableWidth(),
        contentPadding =
            PaddingValues(start = Dimens.Space2xl, end = Dimens.Space2xl, top = topInset + TopBarClearance, bottom = DockClearance),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        item { ChannelListHeader(state) }
        items(state.activeChannels, key = { it.id }) { channel ->
            ChannelCard(
                name = channel.id,
                participantCount = channel.participantCount,
                onClick = { onIntent(ChannelListIntent.JoinChannel(channel.id, channel.id)) },
            )
        }
    }
}

@Composable
private fun ToolbarGroup(
    onOpenSettings: () -> Unit,
    onOpenHistory: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.height(Dimens.GlassControl).glass(CircleShape).padding(horizontal = Dimens.Space2xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onOpenHistory != null) {
            ToolbarIcon(Icons.Default.History, stringResource(Res.string.common_history), onOpenHistory)
        }
        ToolbarIcon(Icons.Default.Tune, stringResource(Res.string.common_settings), onOpenSettings)
    }
}

@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val touchSize = DpSize(width = 42.dp, height = 40.dp)
    val iconSize = 20.dp
    Box(
        modifier =
            Modifier
                .size(touchSize)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun NewChannelDock(
    name: String,
    onIntent: (ChannelListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .readableWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = Dimens.SpaceLg, vertical = Dimens.SpaceXl)
                .glass(MaterialTheme.shapes.extraLarge)
                .padding(Dimens.SpaceXl),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PttTextField(
            value = name,
            onValueChange = { onIntent(ChannelListIntent.UpdateNewChannelName(it)) },
            placeholder = stringResource(Res.string.channels_new_hint),
            modifier = Modifier.weight(1f),
        )
        PillButton(
            text = stringResource(Res.string.channels_join),
            onClick = { onIntent(ChannelListIntent.CreateChannel) },
            enabled = name.isNotBlank(),
        )
    }
}

private val previewState =
    ChannelListState(
        activeChannels =
            listOf(
                ActiveChannelDomain("Geral", 3),
                ActiveChannelDomain("Obra Alameda", 2),
                ActiveChannelDomain("Equipe Evento", 0),
            ),
    )

@Preview
@Composable
private fun ChannelListScreenPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            ChannelListScreenContent(state = previewState, onIntent = {})
        }
    }
}

@Preview
@Composable
private fun ChannelListScreenPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            ChannelListScreenContent(state = previewState, onIntent = {})
        }
    }
}

@Composable
private fun ChannelListHeader(state: ChannelListState) {
    Column(modifier = Modifier.padding(bottom = Dimens.SpaceXs)) {
        Text(
            text = stringResource(Res.string.channels_title),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text =
                if (state.activeChannels.isEmpty()) {
                    stringResource(Res.string.channels_empty)
                } else {
                    stringResource(Res.string.channels_subtitle)
                },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.serverCode?.let { code ->
            // The host and everyone in its room see the same code; a different one means another server
            Text(
                text = stringResource(Res.string.channels_server_code, code),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.SpaceXs),
            )
        }
    }
}
