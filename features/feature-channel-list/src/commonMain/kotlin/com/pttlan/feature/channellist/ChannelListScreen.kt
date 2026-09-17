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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
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
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.repository.ActiveChannelDomain

private val TopBarClearance = 72.dp
private val DockClearance = 120.dp

@Composable
fun ChannelListScreen(
    component: ChannelListComponent,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: (() -> Unit)?,
) {
    val state by component.state.collectAsState()

    ChannelListScreenContent(
        state = state,
        onIntent = component::onIntent,
        onBack = onBack,
        onOpenSettings = onOpenSettings,
        onOpenHistory = onOpenHistory,
    )
}

@Composable
fun ChannelListScreenContent(
    state: ChannelListState,
    onIntent: (ChannelListIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenHistory: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AmbientGlow(
            color = PttTheme.customColors.statusOnline,
            intensity = 0.22f,
            modifier = Modifier.size(440.dp).align(Alignment.TopEnd).offset(160.dp, (-120).dp),
        )
        AmbientGlow(
            color = MaterialTheme.colorScheme.primary,
            intensity = 0.2f,
            modifier = Modifier.size(420.dp).align(Alignment.BottomStart).offset((-180).dp, 80.dp),
        )

        ChannelList(state = state, onIntent = onIntent)

        PttTopBar(
            navigation = { GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Desconectar", onBack) },
            center = { ConnectionStatusBadge(status = ConnectionStatus.Online) },
            actions = { ToolbarGroup(onOpenSettings, onOpenHistory) },
        )

        NewChannelDock(
            name = state.newChannelName,
            onIntent = onIntent,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ChannelList(
    state: ChannelListState,
    onIntent: (ChannelListIntent) -> Unit,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(start = 20.dp, end = 20.dp, top = topInset + TopBarClearance, bottom = DockClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Canais",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text =
                        if (state.activeChannels.isEmpty()) {
                            "Nenhuma sala ativa no momento. Crie uma abaixo."
                        } else {
                            "Salas ativas neste servidor"
                        },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
        modifier = Modifier.height(44.dp).glass(CircleShape).padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onOpenHistory != null) {
            ToolbarIcon(Icons.Default.History, "Histórico", onOpenHistory)
        }
        ToolbarIcon(Icons.Default.Tune, "Configurações", onOpenSettings)
    }
}

@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(width = 42.dp, height = 40.dp)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(20.dp),
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
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .glass(MaterialTheme.shapes.extraLarge)
                .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PttTextField(
            value = name,
            onValueChange = { onIntent(ChannelListIntent.UpdateNewChannelName(it)) },
            placeholder = "Nome do canal",
            modifier = Modifier.weight(1f),
        )
        PillButton(
            text = "Entrar",
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
