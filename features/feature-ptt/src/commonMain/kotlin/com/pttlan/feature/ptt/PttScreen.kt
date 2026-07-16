package com.pttlan.feature.ptt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.ParticipantAvatar
import com.pttlan.core.designsystem.components.PttButton
import com.pttlan.core.designsystem.components.PttButtonState.Idle
import com.pttlan.core.designsystem.components.PttButtonState.Receiving
import com.pttlan.core.designsystem.components.PttButtonState.Requesting
import com.pttlan.core.designsystem.components.PttButtonState.Transmitting
import kotlinx.coroutines.launch

@Composable
fun PttScreen(component: PttComponent) {
    val state by component.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        component.effects.collect { effect ->
            when (effect) {
                is PttEffect.NavigateBack -> {
                    // Handled by Decompose router
                }
                is PttEffect.ShowFloorDenied -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.reason)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Canal: #${state.channelId}",
                style = MaterialTheme.typography.displayLarge,
            )

            Spacer(modifier = Modifier.height(32.dp))

            val pttState =
                if (state.isTransmitting && state.isFloorGranted) {
                    Transmitting
                } else if (state.isTransmitting) {
                    Requesting
                } else if (state.currentSpeakerId != null) {
                    Receiving
                } else {
                    Idle
                }

            PttButton(
                state = pttState,
                onPressStart = { component.onIntent(PttIntent.PressPtt) },
                onPressEnd = { component.onIntent(PttIntent.ReleasePtt) },
                buttonSize = 140.dp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.participants, key = { it.userId }) { participant ->
                    val isSpeaking = participant.userId == state.currentSpeakerId
                    val isRequesting = participant.userId == state.localUserId && state.isTransmitting && !state.isFloorGranted
                    ParticipantAvatar(
                        name = participant.nickname,
                        isSpeaking = isSpeaking,
                        isRequesting = isRequesting,
                    )
                }
            }

            Button(onClick = { component.onIntent(PttIntent.LeaveChannel) }) {
                Text("Sair do Canal")
            }
        }
    }
}
