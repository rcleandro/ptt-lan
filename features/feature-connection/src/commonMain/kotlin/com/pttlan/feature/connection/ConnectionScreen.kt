package com.pttlan.feature.connection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ServerNode

@Composable
fun ConnectionScreen(component: ConnectionComponent) {
    val state by component.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        component.effects.collect { effect ->
            when (effect) {
                is ConnectionEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                ConnectionEffect.NavigateToChannelList -> {
                    // Handled by RootComponent via Decompose navigation in a real app,
                    // but keeping it here for completeness of the component contract
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "PTT LAN",
                style = MaterialTheme.typography.headlineLarge
            )

            if (state.status == ConnectionStatus.Connecting || state.status == ConnectionStatus.Reconnecting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Conectando...")
                }
            } else {
                Text(
                    text = "Servidores Descobertos na Rede",
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (state.discoveredServers.isEmpty()) {
                    Text(
                        text = "Procurando servidores...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.discoveredServers) { server ->
                            ServerCard(server = server) {
                                component.onIntent(ConnectionIntent.ConnectToDiscovered(server))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Conectar Manualmente",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.manualIp,
                        onValueChange = { component.onIntent(ConnectionIntent.UpdateManualIp(it)) },
                        label = { Text("IP do Servidor") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Button(
                        onClick = { component.onIntent(ConnectionIntent.ConnectToManualIp(state.manualIp)) },
                        enabled = state.manualIp.isNotBlank()
                    ) {
                        Text("Conectar")
                    }
                }
            }
        }
    }
}

@Composable
fun ServerCard(server: ServerNode, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = server.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${server.host}:${server.port}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
