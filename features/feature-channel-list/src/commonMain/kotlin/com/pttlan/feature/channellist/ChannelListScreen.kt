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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.ChannelCard
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.repository.ActiveChannelDomain

@Composable
fun ChannelListScreen(component: ChannelListComponent) {
    val state by component.state.collectAsState()

    ChannelListScreenContent(
        state = state,
        onIntent = component::onIntent,
    )
}

@Composable
fun ChannelListScreenContent(
    state: ChannelListState,
    onIntent: (ChannelListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .then(modifier),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.newChannelName,
                onValueChange = { onIntent(ChannelListIntent.UpdateNewChannelName(it)) },
                label = { Text("Nome do Canal") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )

            Button(
                onClick = { onIntent(ChannelListIntent.CreateChannel) },
                enabled = state.newChannelName.isNotBlank(),
            ) {
                Text("Criar/Entrar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Salas Ativas",
            style = MaterialTheme.typography.headlineMedium,
        )

        if (state.activeChannels.isEmpty()) {
            Text(
                text = "Nenhuma sala ativa no momento.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.activeChannels) { channel ->
                    ChannelCard(
                        name = channel.id, // Displaying ID as name for now, could be formatted
                        id = channel.id,
                        participantCount = channel.participantCount,
                        isActive = true,
                        onClick = {
                            onIntent(ChannelListIntent.JoinChannel(channel.id, channel.id))
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ChannelListScreenPreview() {
    PttTheme {
        Surface {
            ChannelListScreenContent(
                state =
                    ChannelListState(
                        activeChannels =
                            listOf(
                                ActiveChannelDomain("Geral", 10),
                                ActiveChannelDomain("Bate papo", 2),
                            ),
                    ),
                onIntent = {},
            )
        }
    }
}
