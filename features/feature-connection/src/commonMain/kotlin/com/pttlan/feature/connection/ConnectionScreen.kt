package com.pttlan.feature.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.AmbientGlow
import com.pttlan.core.designsystem.components.GlassIconButton
import com.pttlan.core.designsystem.components.LabeledTextField
import com.pttlan.core.designsystem.components.PillButton
import com.pttlan.core.designsystem.components.PttTextField
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.StatusDot
import com.pttlan.core.designsystem.components.contentCard
import com.pttlan.core.designsystem.components.glass
import com.pttlan.core.designsystem.components.snackbar.PttSnackbarType
import com.pttlan.core.designsystem.components.snackbar.SnackbarController
import com.pttlan.core.designsystem.components.snackbar.SnackbarEvent
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode

private val DockClearance = 150.dp

@Composable
fun ConnectionScreen(
    component: ConnectionComponent,
    onOpenSettings: () -> Unit,
    onOpenHistory: (() -> Unit)?,
) {
    val state by component.state.collectAsState()

    LaunchedEffect(Unit) {
        component.effects.collect { effect ->
            if (effect is ConnectionEffect.ShowError) {
                SnackbarController.sendEvent(
                    SnackbarEvent(
                        message = effect.message,
                        type = PttSnackbarType.ErrorOrWarning,
                    ),
                )
            }
        }
    }

    ConnectionScreenContent(
        state = state,
        onIntent = component::onIntent,
        onOpenSettings = onOpenSettings,
        onOpenHistory = onOpenHistory,
    )
}

@Composable
fun ConnectionScreenContent(
    state: ConnectionState,
    onIntent: (ConnectionIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenHistory: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AmbientGlow(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(460.dp).offset((-140).dp, (-160).dp),
        )
        AmbientGlow(
            color = PttTheme.customColors.statusOnline,
            intensity = 0.16f,
            modifier = Modifier.size(400.dp).align(Alignment.BottomEnd).offset(170.dp, (-60).dp),
        )

        if (state.status == ConnectionStatus.Connecting || state.status == ConnectionStatus.Reconnecting) {
            ConnectingIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            ServerList(state, onIntent, onOpenSettings, onOpenHistory)
            ManualConnectDock(
                manualIp = state.manualIp,
                onIntent = onIntent,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun ConnectingIndicator(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text("Conectando…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun ServerList(
    state: ConnectionState,
    onIntent: (ConnectionIntent) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: (() -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = DockClearance),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header(onOpenSettings, onOpenHistory) }
        item {
            LabeledTextField(
                label = "Seu nome",
                value = state.nickname,
                onValueChange = { onIntent(ConnectionIntent.UpdateNickname(it)) },
            )
        }
        if (state.canHost) {
            item { HostCard(onHost = { onIntent(ConnectionIntent.HostServer) }) }
        }
        item { DiscoveryHeader(isSearching = state.discoveredServers.isEmpty()) }
        items(state.discoveredServers) { server ->
            ServerCard(server = server) { onIntent(ConnectionIntent.ConnectToDiscovered(server)) }
        }
    }
}

@Composable
private fun Header(
    onOpenSettings: () -> Unit,
    onOpenHistory: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Conectar",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (onOpenHistory != null) {
                GlassIconButton(icon = Icons.Default.History, contentDescription = "Histórico", onClick = onOpenHistory)
            }
            GlassIconButton(icon = Icons.Default.Tune, contentDescription = "Configurações", onClick = onOpenSettings)
        }
        Text(
            text = "Escolha um servidor na rede ou digite o endereço.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HostCard(onHost: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().contentCard().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hospedar nesta máquina",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Os outros encontram o canal na rede, sem servidor à parte.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PillButton(text = "Hospedar", onClick = onHost)
    }
}

@Composable
private fun DiscoveryHeader(isSearching: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(text = "Na rede", modifier = Modifier.weight(1f).padding(start = 4.dp))
        if (isSearching) {
            Row(
                modifier = Modifier.glass(CircleShape).padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(color = MaterialTheme.colorScheme.primary, halo = true)
                SectionLabel(text = "procurando")
            }
        }
    }
}

@Composable
fun ServerCard(
    server: ServerNode,
    onClick: () -> Unit,
) {
    val colors = PttTheme.customColors
    val isLocal = server.endpoint.isLocal
    val accent = if (isLocal) MaterialTheme.colorScheme.primary else colors.accentTx

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .contentCard()
                .clickable(onClick = onClick)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(if (isLocal) colors.primaryGlow else colors.accentTxGlow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isLocal) Icons.Default.Dns else Icons.Default.Language,
                contentDescription = null,
                tint = accent,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${server.endpoint.host} : ${server.endpoint.port}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionLabel(text = if (isLocal) "LAN" else "WEB", color = accent)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
        )
    }
}

@Composable
private fun ManualConnectDock(
    manualIp: String,
    onIntent: (ConnectionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 16.dp)
                .glass(MaterialTheme.shapes.extraLarge)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Conectar manualmente",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PttTextField(
                value = manualIp,
                onValueChange = { onIntent(ConnectionIntent.UpdateManualIp(it)) },
                placeholder = "IP ou domínio",
                monospace = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = "Conectar",
                onClick = { onIntent(ConnectionIntent.ConnectToManualIp(manualIp)) },
                enabled = manualIp.isNotBlank(),
            )
        }
    }
}

private val previewState =
    ConnectionState(
        status = ConnectionStatus.Disconnected,
        nickname = "Leandro",
        manualIp = "",
        canHost = true,
        discoveredServers =
            listOf(
                ServerNode("PTT-LAN-Server-4821", ServerEndpoint("192.168.0.12", 9443, true)),
                ServerNode("Favorito", ServerEndpoint("ptt.exemplo.com.br", 9443, false)),
            ),
    )

@Preview
@Composable
private fun ConnectionScreenPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            ConnectionScreenContent(state = previewState, onIntent = {})
        }
    }
}

@Preview
@Composable
private fun ConnectionScreenPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            ConnectionScreenContent(state = previewState, onIntent = {})
        }
    }
}
