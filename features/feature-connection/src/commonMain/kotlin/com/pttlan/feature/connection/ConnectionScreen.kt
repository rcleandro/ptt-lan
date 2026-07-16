package com.pttlan.feature.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.ConnectionStatus.Online
import com.pttlan.core.designsystem.components.ConnectionStatusBadge
import com.pttlan.core.designsystem.components.snackbar.PttSnackbarType
import com.pttlan.core.designsystem.components.snackbar.SnackbarController
import com.pttlan.core.designsystem.components.snackbar.SnackbarEvent
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerNode

@Composable
fun ConnectionScreen(component: ConnectionComponent) {
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
    )
}

@Composable
fun ConnectionScreenContent(
    state: ConnectionState,
    onIntent: (ConnectionIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.status == ConnectionStatus.Connecting || state.status == ConnectionStatus.Reconnecting) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(modifier),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Conectando...")
            }
        }
    } else {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .then(modifier),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.nickname,
                onValueChange = { onIntent(ConnectionIntent.UpdateNickname(it)) },
                label = { Text("Seu Nome") },
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Servidores Descobertos na Rede",
                style = MaterialTheme.typography.headlineMedium,
            )

            if (state.discoveredServers.isEmpty()) {
                Text(
                    text = "Procurando servidores...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.discoveredServers) { server ->
                        ServerCard(server = server) {
                            onIntent(ConnectionIntent.ConnectToDiscovered(server))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Conectar Manualmente",
                style = MaterialTheme.typography.headlineMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.manualIp,
                    onValueChange = { onIntent(ConnectionIntent.UpdateManualIp(it)) },
                    label = { Text("IP do Servidor") },
                    modifier = Modifier.weight(1f),
                )

                Button(
                    onClick = { onIntent(ConnectionIntent.ConnectToManualIp(state.manualIp)) },
                    enabled = state.manualIp.isNotBlank(),
                ) {
                    Text("Conectar")
                }
            }
        }
    }
}

@Composable
fun ServerCard(
    server: ServerNode,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(PttTheme.customColors.surface2)
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = server.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "${server.host}:${server.port}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ConnectionStatusBadge(
            status = Online,
        )
    }
}

@Preview
@Composable
private fun ConnectionScreenPreviewDark() {
    PttTheme(appTheme = com.pttlan.core.designsystem.theme.AppTheme.DARK) {
        Surface {
            ConnectionScreenContent(
                state =
                    ConnectionState(
                        status = ConnectionStatus.Disconnected,
                        nickname = "Leandro",
                        manualIp = "192.168.0.1",
                        discoveredServers =
                            listOf(
                                ServerNode("Servidor Local", "192.168.0.10", 9443),
                                ServerNode("Servidor Remoto", "10.0.0.5", 9443),
                            ),
                    ),
                onIntent = {},
            )
        }
    }
}

@Preview
@Composable
private fun ConnectionScreenPreviewLight() {
    PttTheme(appTheme = com.pttlan.core.designsystem.theme.AppTheme.LIGHT) {
        Surface {
            ConnectionScreenContent(
                state =
                    ConnectionState(
                        status = ConnectionStatus.Disconnected,
                        nickname = "Leandro",
                        manualIp = "192.168.0.1",
                        discoveredServers =
                            listOf(
                                ServerNode("Servidor Local", "192.168.0.10", 9443),
                                ServerNode("Servidor Remoto", "10.0.0.5", 9443),
                            ),
                    ),
                onIntent = {},
            )
        }
    }
}
