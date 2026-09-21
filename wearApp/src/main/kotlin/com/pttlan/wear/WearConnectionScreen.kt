package com.pttlan.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.ui.tooling.preview.Preview
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerEndpoint
import com.pttlan.domain.ptt.repository.ServerNode
import com.pttlan.feature.connection.ConnectionComponent
import com.pttlan.feature.connection.ConnectionEffect
import com.pttlan.feature.connection.ConnectionIntent
import com.pttlan.feature.connection.ConnectionState

@Composable
fun WearConnectionScreen(component: ConnectionComponent) {
    val state by component.state.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(component) {
        component.effects.collect { if (it is ConnectionEffect.ShowError) error = it.message }
    }
    val onIntent: (ConnectionIntent) -> Unit = {
        if (it is ConnectionIntent.ConnectToDiscovered || it is ConnectionIntent.ConnectToManualIp) error = null
        component.onIntent(it)
    }
    WearConnectionContent(
        state = state,
        error = error,
        onIntent = onIntent,
        onEditName = rememberTextInput("Seu nome") { onIntent(ConnectionIntent.UpdateNickname(it)) },
        onEditPin = rememberTextInput("PIN da sala") { onIntent(ConnectionIntent.UpdatePin(it)) },
        onEnterIp =
            rememberTextInput("IP do servidor") {
                onIntent(ConnectionIntent.UpdateManualIp(it))
                onIntent(ConnectionIntent.ConnectToManualIp(it.trim()))
            },
    )
}

@Composable
fun WearConnectionContent(
    state: ConnectionState,
    error: String?,
    onIntent: (ConnectionIntent) -> Unit,
    onEditName: () -> Unit,
    onEditPin: () -> Unit,
    onEnterIp: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text("PTT-LAN") } }
            item {
                FilledTonalButton(
                    onClick = onEditName,
                    label = { Text(state.nickname.ifBlank { "Defina seu nome" }) },
                    secondaryLabel = { Text("Nome") },
                )
            }
            item {
                FilledTonalButton(
                    onClick = onEditPin,
                    label = { Text(if (state.pin.isBlank()) "Sem PIN" else "•".repeat(state.pin.length)) },
                    secondaryLabel = { Text("PIN da sala") },
                )
            }
            item { ListHeader { Text(if (state.status == ConnectionStatus.Connecting) "Conectando…" else "Na rede") } }
            items(state.discoveredServers.size) { index ->
                val server = state.discoveredServers[index]
                Button(
                    onClick = { onIntent(ConnectionIntent.ConnectToDiscovered(server)) },
                    label = { Text(server.name.removePrefix("PTT-LAN-")) },
                    secondaryLabel = { Text(server.endpoint.host) },
                )
            }
            item {
                FilledTonalButton(
                    onClick = { onIntent(ConnectionIntent.RefreshServers) },
                    label = { Text(if (state.discoveredServers.isEmpty()) "Procurando… tocar p/ repetir" else "Procurar de novo") },
                )
            }
            item { FilledTonalButton(onClick = onEnterIp, label = { Text("Digitar IP") }) }
            error?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun ConnectionPreview(
    state: ConnectionState,
    error: String? = null,
) {
    WearPttTheme { AppScaffold { WearConnectionContent(state, error, {}, {}, {}, {}) } }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Procurando")
@Composable
private fun ConnectionSearchingPreview() {
    ConnectionPreview(ConnectionState())
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Servidores encontrados")
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Servidores (relógio pequeno)")
@Composable
private fun ConnectionFoundPreview() {
    ConnectionPreview(
        state =
            ConnectionState(
                nickname = "Leandro",
                pin = "4821",
                discoveredServers =
                    listOf(
                        ServerNode("PTT-LAN-Android", ServerEndpoint("192.168.1.230", 9443, isLocal = true)),
                        ServerNode("PTT-LAN-Server-1790013806354", ServerEndpoint("192.168.1.231", 9443, isLocal = true)),
                    ),
            ),
        error = "PIN da sala incorreto",
    )
}
