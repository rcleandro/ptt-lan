package com.pttlan.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.pttlan.feature.ptt.PttComponent
import com.pttlan.feature.ptt.PttEffect
import com.pttlan.feature.ptt.PttIntent
import kotlinx.coroutines.delay

private val PttButtonSize = 116.dp
private const val FLOOR_DENIED_VISIBLE_MS = 2_000L

@Composable
fun WearPttScreen(component: PttComponent) {
    val state by component.state.collectAsState()
    val haptics = LocalHapticFeedback.current
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

    val colors = MaterialTheme.colorScheme
    val talking = state.isTransmitting || state.isFloorGranted
    val status =
        when {
            denied != null -> denied!!
            talking -> "Falando…"
            state.currentSpeakerName != null -> "${state.currentSpeakerName} falando"
            else -> "Segure para falar"
        }

    // The padding keeps the top clear of the time the watch draws there, and the rest inside the round screen
    ScreenScaffold { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text("${state.channelId} · ${state.participants.size}", style = MaterialTheme.typography.labelMedium)
            Box(
                modifier =
                    Modifier
                        .size(PttButtonSize)
                        .clip(CircleShape)
                        .background(
                            when {
                                talking -> colors.error
                                state.floorBlocked -> colors.surfaceContainerHigh
                                else -> colors.primary
                            },
                        ).pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    component.onIntent(PttIntent.PressPtt)
                                    tryAwaitRelease()
                                    component.onIntent(PttIntent.ReleasePtt)
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                            )
                        },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    status,
                    textAlign = TextAlign.Center,
                    color = if (talking) colors.onError else colors.onPrimary,
                )
            }
            CompactButton(onClick = { component.onIntent(PttIntent.LeaveChannel) }, label = { Text("Sair") })
        }
    }
}
