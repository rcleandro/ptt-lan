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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.ParticipantAvatar
import com.pttlan.core.designsystem.components.PttButton
import com.pttlan.core.designsystem.components.PttButtonState.Idle
import com.pttlan.core.designsystem.components.PttButtonState.Receiving
import com.pttlan.core.designsystem.components.PttButtonState.Requesting
import com.pttlan.core.designsystem.components.PttButtonState.Transmitting
import com.pttlan.core.designsystem.components.snackbar.PttSnackbarType
import com.pttlan.core.designsystem.components.snackbar.SnackbarController
import com.pttlan.core.designsystem.components.snackbar.SnackbarEvent
import com.pttlan.core.designsystem.theme.PttTheme

@Composable
fun PttScreen(component: PttComponent) {
    val state by component.state.collectAsState()

    LaunchedEffect(Unit) {
        component.effects.collect { effect ->
            if (effect is PttEffect.ShowFloorDenied) {
                SnackbarController.sendEvent(
                    SnackbarEvent(
                        message = effect.reason,
                        type = PttSnackbarType.ErrorOrWarning,
                    ),
                )
            }
        }
    }

    PttScreenContent(
        state = state,
        onIntent = component::onIntent,
    )
}

@Composable
fun PttScreenContent(
    state: PttState,
    onIntent: (PttIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .then(modifier),
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
            onPressStart = { onIntent(PttIntent.PressPtt) },
            onPressEnd = { onIntent(PttIntent.ReleasePtt) },
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

        Button(onClick = { onIntent(PttIntent.LeaveChannel) }) {
            Text("Sair do Canal")
        }
    }
}

@Preview
@Composable
private fun PttScreenPreviewDark() {
    PttTheme(appTheme = com.pttlan.core.designsystem.theme.AppTheme.DARK) {
        Surface {
            PttScreenContent(
                state =
                    PttState(
                        channelId = "Geral",
                        localUserId = "u1",
                        isTransmitting = false,
                        currentSpeakerId = "u2",
                        currentSpeakerName = "João",
                        participants =
                            listOf(
                                com.pttlan.core.network.protocol
                                    .ParticipantDto("u1", "Leandro", false),
                                com.pttlan.core.network.protocol
                                    .ParticipantDto("u2", "João", true),
                                com.pttlan.core.network.protocol
                                    .ParticipantDto("u3", "Maria", false),
                            ),
                    ),
                onIntent = {},
            )
        }
    }
}

@Preview
@Composable
private fun PttScreenPreviewLight() {
    PttTheme(appTheme = com.pttlan.core.designsystem.theme.AppTheme.LIGHT) {
        Surface {
            PttScreenContent(
                state =
                    PttState(
                        channelId = "Geral",
                        localUserId = "u1",
                        isTransmitting = false,
                        currentSpeakerId = "u2",
                        currentSpeakerName = "João",
                        participants =
                            listOf(
                                com.pttlan.core.network.protocol
                                    .ParticipantDto("u1", "Leandro", false),
                                com.pttlan.core.network.protocol
                                    .ParticipantDto("u2", "João", true),
                                com.pttlan.core.network.protocol
                                    .ParticipantDto("u3", "Maria", false),
                            ),
                    ),
                onIntent = {},
            )
        }
    }
}
