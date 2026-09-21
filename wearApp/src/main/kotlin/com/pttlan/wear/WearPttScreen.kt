package com.pttlan.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.material3.timeTextSeparator
import com.pttlan.core.designsystem.components.PttButton
import com.pttlan.core.designsystem.components.PttButtonState
import com.pttlan.feature.ptt.PttComponent
import com.pttlan.feature.ptt.PttEffect
import com.pttlan.feature.ptt.PttIntent
import com.pttlan.feature.ptt.buttonState
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.tooling.preview.devices.WearDevices
import com.pttlan.domain.ptt.model.ParticipantDomain
import com.pttlan.feature.ptt.PttState
import kotlinx.coroutines.delay

private val PttButtonSize = 112.dp
private val PttButtonMargin = 6.dp
private const val FLOOR_DENIED_VISIBLE_MS = 2_000L

@Composable
fun WearPttScreen(component: PttComponent) {
    val state by component.state.collectAsState()
    var denied by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(component) {
        component.effects.collect { if (it is PttEffect.ShowFloorDenied) denied = it.reason }
    }
    LaunchedEffect(denied) {
        if (denied != null) {
            delay(FLOOR_DENIED_VISIBLE_MS)
            denied = null
        }
    }
    WearPttContent(state, denied, component::onIntent)
}

@Composable
fun WearPttContent(
    state: PttState,
    denied: String?,
    onIntent: (PttIntent) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    // Same rule as the phone: pressed, waiting for the floor, talking, or someone else talking
    val buttonState = state.buttonState()
    val status =
        denied ?: when (buttonState) {
            PttButtonState.Transmitting -> "Falando…"
            PttButtonState.Requesting -> "Pedindo a vez…"
            PttButtonState.Receiving -> "${state.currentSpeakerName ?: "Alguém"} falando"
            PttButtonState.Idle -> "Segure para falar"
        }

    // The channel goes along the curved edge next to the time, leaving the middle to the button
    ScreenScaffold(
        timeText = {
            TimeText { time ->
                timeTextCurvedText(time)
                timeTextSeparator()
                timeTextCurvedText("${state.channelId} · ${state.participants.size}")
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            PttButton(
                state = buttonState,
                onPressStart = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onIntent(PttIntent.PressPtt)
                },
                onPressEnd = {
                    onIntent(PttIntent.ReleasePtt)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                buttonSize = PttButtonSize,
                buttonMargin = PttButtonMargin,
            )
            Text(
                status,
                style = MaterialTheme.typography.labelMedium,
                color = if (denied != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            CompactButton(onClick = { onIntent(PttIntent.LeaveChannel) }, label = { Text("Sair") })
        }
    }
}

private val PreviewParticipants =
    listOf(
        ParticipantDomain("me", "Leandro", isSpeaking = false),
        ParticipantDomain("ana", "Ana", isSpeaking = false),
        ParticipantDomain("rui", "Rui", isSpeaking = false),
    )

@Composable
private fun PttPreview(
    state: PttState,
    denied: String? = null,
) {
    WearPttTheme { AppScaffold { WearPttContent(state, denied, {}) } }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Parado")
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "Parado (relógio pequeno)")
@Composable
private fun PttIdlePreview() {
    PttPreview(PttState(channelId = "Geral", localUserId = "me", participants = PreviewParticipants))
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Pedindo a vez")
@Composable
private fun PttRequestingPreview() {
    PttPreview(PttState(channelId = "Geral", localUserId = "me", isTransmitting = true, participants = PreviewParticipants))
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Falando")
@Composable
private fun PttTransmittingPreview() {
    PttPreview(
        PttState(
            channelId = "Geral",
            localUserId = "me",
            isTransmitting = true,
            isFloorGranted = true,
            currentSpeakerId = "me",
            participants = PreviewParticipants,
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Outra pessoa falando")
@Composable
private fun PttReceivingPreview() {
    PttPreview(
        PttState(
            channelId = "Geral",
            localUserId = "me",
            currentSpeakerId = "ana",
            currentSpeakerName = "Ana",
            floorBlocked = true,
            participants = PreviewParticipants,
        ),
    )
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true, name = "Canal ocupado")
@Composable
private fun PttDeniedPreview() {
    PttPreview(PttState(channelId = "Geral", localUserId = "me", participants = PreviewParticipants), denied = "Canal ocupado")
}
