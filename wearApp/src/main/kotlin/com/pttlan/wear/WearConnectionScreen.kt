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
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.feature.connection.ConnectionComponent
import com.pttlan.feature.connection.ConnectionEffect
import com.pttlan.feature.connection.ConnectionIntent

@Composable
fun WearConnectionScreen(component: ConnectionComponent) {
    val state by component.state.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(component) {
        component.effects.collect { if (it is ConnectionEffect.ShowError) error = it.message }
    }
    val editName = rememberTextInput("Seu nome") { component.onIntent(ConnectionIntent.UpdateNickname(it)) }
    val editPin = rememberTextInput("PIN da sala") { component.onIntent(ConnectionIntent.UpdatePin(it)) }
    val enterIp =
        rememberTextInput("IP do servidor") {
            error = null
            component.onIntent(ConnectionIntent.UpdateManualIp(it))
            component.onIntent(ConnectionIntent.ConnectToManualIp(it.trim()))
        }

    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text("PTT-LAN") } }
            item {
                FilledTonalButton(
                    onClick = editName,
                    label = { Text(state.nickname.ifBlank { "Defina seu nome" }) },
                    secondaryLabel = { Text("Nome") },
                )
            }
            item {
                FilledTonalButton(
                    onClick = editPin,
                    label = { Text(if (state.pin.isBlank()) "Sem PIN" else "•".repeat(state.pin.length)) },
                    secondaryLabel = { Text("PIN da sala") },
                )
            }
            item { ListHeader { Text(if (state.status == ConnectionStatus.Connecting) "Conectando…" else "Na rede") } }
            items(state.discoveredServers.size) { index ->
                val server = state.discoveredServers[index]
                Button(
                    onClick = {
                        error = null
                        component.onIntent(ConnectionIntent.ConnectToDiscovered(server))
                    },
                    label = { Text(server.name.removePrefix("PTT-LAN-")) },
                    secondaryLabel = { Text(server.endpoint.host) },
                )
            }
            item {
                FilledTonalButton(
                    onClick = { component.onIntent(ConnectionIntent.RefreshServers) },
                    label = { Text(if (state.discoveredServers.isEmpty()) "Procurando… tocar p/ repetir" else "Procurar de novo") },
                )
            }
            item { FilledTonalButton(onClick = enterIp, label = { Text("Digitar IP") }) }
            error?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
