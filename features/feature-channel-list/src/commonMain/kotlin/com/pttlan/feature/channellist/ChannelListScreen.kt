package com.pttlan.feature.channellist

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChannelListScreen(component: ChannelListComponent) {
    val state by component.state.collectAsState()

    LaunchedEffect(Unit) {
        component.effects.collect { effect ->
            when (effect) {
                is ChannelListEffect.NavigateToChannel -> {
                    // Handled by Decompose router
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Canais",
                style = MaterialTheme.typography.displayLarge,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.newChannelName,
                    onValueChange = { component.onIntent(ChannelListIntent.UpdateNewChannelName(it)) },
                    label = { Text("Nome do Canal") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )

                Button(
                    onClick = { component.onIntent(ChannelListIntent.CreateChannel) },
                    enabled = state.newChannelName.isNotBlank(),
                ) {
                    Text("Criar/Entrar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Recentes",
                style = MaterialTheme.typography.headlineMedium,
            )

            if (state.recentChannels.isEmpty()) {
                Text(
                    text = "Nenhum canal recente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.recentChannels) { channel ->
                        com.pttlan.core.designsystem.components.ChannelCard(
                            name = channel.name,
                            id = channel.id,
                            participantCount = 0,
                            isActive = true,
                            onClick = {
                                component.onIntent(ChannelListIntent.JoinChannel(channel.id, channel.name))
                            },
                        )
                    }
                }
            }
        }
    }
}
