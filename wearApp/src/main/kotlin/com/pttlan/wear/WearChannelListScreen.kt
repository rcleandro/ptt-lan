package com.pttlan.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.pttlan.feature.channellist.ChannelListComponent
import com.pttlan.feature.channellist.ChannelListIntent

@Composable
fun WearChannelListScreen(component: ChannelListComponent) {
    val state by component.state.collectAsState()
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text("Canais") } }
            items(state.activeChannels.size) { index ->
                val channel = state.activeChannels[index]
                Button(
                    onClick = { component.onIntent(ChannelListIntent.JoinChannel(channel.id, channel.id)) },
                    label = { Text(channel.id) },
                    secondaryLabel = { Text(if (channel.participantCount == 1) "1 pessoa" else "${channel.participantCount} pessoas") },
                )
            }
            item {
                FilledTonalButton(
                    onClick = { component.onIntent(ChannelListIntent.Leave) },
                    label = { Text("Sair do servidor") },
                )
            }
        }
    }
}
