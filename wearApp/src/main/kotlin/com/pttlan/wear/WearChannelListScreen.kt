package com.pttlan.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.pttlan.domain.ptt.repository.ActiveChannelDomain
import com.pttlan.feature.channellist.ChannelListComponent
import com.pttlan.feature.channellist.ChannelListIntent
import com.pttlan.feature.channellist.ChannelListState

@Composable
fun WearChannelListScreen(component: ChannelListComponent) {
    val state by component.state.collectAsState()
    WearChannelListContent(state, component::onIntent)
}

@Composable
fun WearChannelListContent(
    state: ChannelListState,
    onIntent: (ChannelListIntent) -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text("Canais") } }
            items(state.activeChannels.size) { index ->
                val channel = state.activeChannels[index]
                Button(
                    onClick = { onIntent(ChannelListIntent.JoinChannel(channel.id, channel.id)) },
                    label = { Text(channel.id) },
                    secondaryLabel = { Text(if (channel.participantCount == 1) "1 pessoa" else "${channel.participantCount} pessoas") },
                )
            }
            item {
                FilledTonalButton(
                    onClick = { onIntent(ChannelListIntent.Leave) },
                    label = { Text("Sair do servidor") },
                )
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Canais")
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Canais (relógio pequeno)")
@Composable
private fun ChannelListPreview() {
    WearPttTheme {
        AppScaffold {
            WearChannelListContent(
                state =
                    ChannelListState(
                        activeChannels =
                            listOf(ActiveChannelDomain("Geral", 3), ActiveChannelDomain("Obra", 1), ActiveChannelDomain("Portaria", 0)),
                    ),
                onIntent = {},
            )
        }
    }
}
