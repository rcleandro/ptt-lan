package com.pttlan.feature.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Refresh
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
import com.pttlan.core.common.MIN_ROOM_PIN_LENGTH
import com.pttlan.core.designsystem.components.AmbientGlow
import com.pttlan.core.designsystem.components.GlassIconButton
import com.pttlan.core.designsystem.components.LabeledTextField
import com.pttlan.core.designsystem.components.PillButton
import com.pttlan.core.designsystem.components.PttTextField
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.StatusDot
import com.pttlan.core.designsystem.components.contentCard
import com.pttlan.core.designsystem.components.glass
import com.pttlan.core.designsystem.components.readableWidth
import com.pttlan.core.designsystem.components.snackbar.PttSnackbarType
import com.pttlan.core.designsystem.components.snackbar.SnackbarController
import com.pttlan.core.designsystem.components.snackbar.SnackbarEvent
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.common_history
import com.pttlan.core.designsystem.generated.resources.common_settings
import com.pttlan.core.designsystem.generated.resources.connection_connect
import com.pttlan.core.designsystem.generated.resources.connection_connecting
import com.pttlan.core.designsystem.generated.resources.connection_host
import com.pttlan.core.designsystem.generated.resources.connection_host_description
import com.pttlan.core.designsystem.generated.resources.connection_host_title
import com.pttlan.core.designsystem.generated.resources.connection_manual
import com.pttlan.core.designsystem.generated.resources.connection_manual_hint
import com.pttlan.core.designsystem.generated.resources.connection_nickname
import com.pttlan.core.designsystem.generated.resources.connection_on_network
import com.pttlan.core.designsystem.generated.resources.connection_pin
import com.pttlan.core.designsystem.generated.resources.connection_search_again
import com.pttlan.core.designsystem.generated.resources.connection_searching
import com.pttlan.core.designsystem.generated.resources.connection_server_address
import com.pttlan.core.designsystem.generated.resources.connection_server_lan
import com.pttlan.core.designsystem.generated.resources.connection_server_web
import com.pttlan.core.designsystem.generated.resources.connection_subtitle
import com.pttlan.core.designsystem.generated.resources.connection_title
import com.pttlan.core.designsystem.resolveString
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode
import org.jetbrains.compose.resources.stringResource

private val DockClearance = 150.dp
private val TopGlowSize = 460.dp
private val TopGlowOffsetX = (-140).dp
private val TopGlowOffsetY = (-160).dp
private val BottomGlowSize = 400.dp
private val BottomGlowOffsetX = 170.dp
private val BottomGlowOffsetY = (-60).dp
private const val BOTTOM_GLOW_INTENSITY = 0.16f
private val ServerIconBoxSize = 44.dp
private val DockSpacing = 10.dp

/** Below this height (a Flip's cover screen) a floating dock would cover half the list, so it scrolls with it. */
private val ShortHeight = 480.dp

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
                        message = resolveString(effect.message, effect.args),
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

    state.certificateChange?.let { change ->
        CertificateChangeDialog(
            change = change,
            onTrust = { component.onIntent(ConnectionIntent.TrustNewCertificate) },
            onDismiss = { component.onIntent(ConnectionIntent.DismissCertificateChange) },
        )
    }
}

@Composable
fun ConnectionScreenContent(
    state: ConnectionState,
    onIntent: (ConnectionIntent) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenHistory: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val dockInList = maxHeight < ShortHeight
        val dock = @Composable { dockModifier: Modifier ->
            ManualConnectDock(manualIp = state.manualIp, onIntent = onIntent, modifier = dockModifier)
        }
        AmbientGlow(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(TopGlowSize).offset(TopGlowOffsetX, TopGlowOffsetY),
        )
        AmbientGlow(
            color = PttTheme.customColors.statusOnline,
            intensity = BOTTOM_GLOW_INTENSITY,
            modifier = Modifier.size(BottomGlowSize).align(Alignment.BottomEnd).offset(BottomGlowOffsetX, BottomGlowOffsetY),
        )

        if (state.status == ConnectionStatus.Connecting || state.status == ConnectionStatus.Reconnecting) {
            ConnectingIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            ServerList(state, onIntent, onOpenSettings, onOpenHistory, footer = if (dockInList) dock else null)
            if (!dockInList) dock(Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun ConnectingIndicator(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXl),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(Res.string.connection_connecting),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ServerList(
    state: ConnectionState,
    onIntent: (ConnectionIntent) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: (() -> Unit)?,
    footer: (@Composable (Modifier) -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().readableWidth().windowInsetsPadding(WindowInsets.statusBars),
        contentPadding =
            PaddingValues(
                start = Dimens.Space2xl,
                end = Dimens.Space2xl,
                top = Dimens.SpaceXl,
                bottom =
                    if (footer ==
                        null
                    ) {
                        DockClearance
                    } else {
                        0.dp
                    },
            ),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        item { Header(onOpenSettings, onOpenHistory) }
        item { NicknameField(state.nickname, onIntent) }
        item { PinField(state.pin, onIntent) }
        if (state.canHost) {
            item { HostCard(onHost = { onIntent(ConnectionIntent.HostServer) }) }
        }
        item {
            DiscoveryHeader(
                isSearching = state.discoveredServers.isEmpty(),
                onRefresh = { onIntent(ConnectionIntent.RefreshServers) },
            )
        }
        items(state.discoveredServers) { server ->
            ServerCard(server = server) { onIntent(ConnectionIntent.ConnectToDiscovered(server)) }
        }
        if (footer != null) item { footer(Modifier) }
    }
}

@Composable
private fun Header(
    onOpenSettings: () -> Unit,
    onOpenHistory: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            Text(
                text = stringResource(Res.string.connection_title),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            if (onOpenHistory != null) {
                GlassIconButton(
                    icon = Icons.Default.History,
                    contentDescription = stringResource(Res.string.common_history),
                    onClick = onOpenHistory,
                )
            }
            GlassIconButton(
                icon = Icons.Default.Tune,
                contentDescription = stringResource(Res.string.common_settings),
                onClick = onOpenSettings,
            )
        }
        Text(
            text = stringResource(Res.string.connection_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DiscoveryHeader(
    isSearching: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        SectionLabel(
            text = stringResource(Res.string.connection_on_network),
            modifier = Modifier.weight(1f).padding(start = Dimens.SpaceXs),
        )
        if (isSearching) {
            Row(
                modifier =
                    Modifier
                        .glass(
                            CircleShape,
                        ).padding(start = Dimens.SpaceMd, end = Dimens.SpaceLg, top = Dimens.SpaceXs, bottom = Dimens.SpaceXs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(color = MaterialTheme.colorScheme.primary, halo = true)
                SectionLabel(text = stringResource(Res.string.connection_searching))
            }
        }
        GlassIconButton(
            icon = Icons.Default.Refresh,
            contentDescription = stringResource(Res.string.connection_search_again),
            onClick = onRefresh,
        )
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
                .padding(Dimens.SpaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        Box(
            modifier =
                Modifier
                    .size(ServerIconBoxSize)
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
                text = stringResource(Res.string.connection_server_address, server.endpoint.host, server.endpoint.port),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SectionLabel(
            text = stringResource(if (isLocal) Res.string.connection_server_lan else Res.string.connection_server_web),
            color = accent,
        )
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
                .readableWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = Dimens.SpaceLg, vertical = Dimens.SpaceXl)
                .glass(MaterialTheme.shapes.extraLarge)
                .padding(Dimens.SpaceXl),
        verticalArrangement = Arrangement.spacedBy(DockSpacing),
    ) {
        Text(
            text = stringResource(Res.string.connection_manual),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Dimens.SpaceSm),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd), verticalAlignment = Alignment.CenterVertically) {
            PttTextField(
                value = manualIp,
                onValueChange = { onIntent(ConnectionIntent.UpdateManualIp(it)) },
                placeholder = stringResource(Res.string.connection_manual_hint),
                monospace = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.weight(1f),
            )
            PillButton(
                text = stringResource(Res.string.connection_connect),
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
