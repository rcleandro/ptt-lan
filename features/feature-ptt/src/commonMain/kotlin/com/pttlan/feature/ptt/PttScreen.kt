package com.pttlan.feature.ptt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.GlassIconButton
import com.pttlan.core.designsystem.components.ParticipantAvatar
import com.pttlan.core.designsystem.components.PillButton
import com.pttlan.core.designsystem.components.PillButtonStyle
import com.pttlan.core.designsystem.components.PttButton
import com.pttlan.core.designsystem.components.PttButtonState
import com.pttlan.core.designsystem.components.PttTopBar
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.StatusDot
import com.pttlan.core.designsystem.components.glass
import com.pttlan.core.designsystem.components.snackbar.PttSnackbarType
import com.pttlan.core.designsystem.components.snackbar.SnackbarController
import com.pttlan.core.designsystem.components.snackbar.SnackbarEvent
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.core.network.protocol.ParticipantDto

@Composable
fun PttScreen(
    component: PttComponent,
    onBack: () -> Unit,
    showHistory: Boolean,
) {
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
        onBack = onBack,
        showHistory = showHistory,
    )
}

internal fun PttState.buttonState(): PttButtonState =
    when {
        isTransmitting && isFloorGranted -> PttButtonState.Transmitting
        isTransmitting -> PttButtonState.Requesting
        currentSpeakerId != null && currentSpeakerId != localUserId -> PttButtonState.Receiving
        else -> PttButtonState.Idle
    }

@Composable
fun PttScreenContent(
    state: PttState,
    onIntent: (PttIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    showHistory: Boolean = false,
) {
    val buttonState = state.buttonState()

    BoxWithConstraints(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
        // Landscape phones and Android Automotive head units: talk area and channel area side by side.
        val isWide = maxWidth > maxHeight && maxHeight < CompactHeight
        val buttonSize = if (maxHeight < RegularHeight) 160.dp else 208.dp

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PttTopBar(
                navigation = { GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Voltar para canais", onBack) },
                center = { ChannelTitle(channelId = state.channelId, participantCount = state.participants.size) },
                actions = {
                    if (showHistory) {
                        GlassIconButton(Icons.Default.History, "Histórico", { onIntent(PttIntent.GoToHistory) })
                    }
                },
            )

            if (isWide) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    TalkArea(state, buttonState, buttonSize, onIntent, Modifier.weight(1f))
                    ChannelArea(state, buttonState, onIntent, Modifier.weight(1f))
                }
            } else {
                TalkArea(state, buttonState, buttonSize, onIntent, Modifier.weight(1f))
                ChannelArea(state, buttonState, onIntent)
            }
        }
    }
}

private val CompactHeight = 600.dp
private val RegularHeight = 700.dp

@Composable
private fun TalkArea(
    state: PttState,
    buttonState: PttButtonState,
    buttonSize: Dp,
    onIntent: (PttIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusBlock(buttonState = buttonState, speakerName = state.currentSpeakerName)
        Spacer(modifier = Modifier.height(16.dp))
        PttButton(
            state = buttonState,
            onPressStart = { onIntent(PttIntent.PressPtt) },
            onPressEnd = { onIntent(PttIntent.ReleasePtt) },
            buttonSize = buttonSize,
        )
        Text(
            text = buttonState.hint(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun ChannelArea(
    state: PttState,
    buttonState: PttButtonState,
    onIntent: (PttIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        ParticipantsPanel(state = state, buttonState = buttonState)
        PillButton(
            text = "Sair do canal",
            onClick = { onIntent(PttIntent.LeaveChannel) },
            style = PillButtonStyle.GlassDestructive,
            icon = Icons.AutoMirrored.Filled.Logout,
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }
}

@Composable
private fun ChannelTitle(
    channelId: String,
    participantCount: Int,
) {
    Column(
        modifier = Modifier.glass(CircleShape).padding(horizontal = 22.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "# $channelId",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SectionLabel(text = "$participantCount no canal")
    }
}

@Composable
private fun StatusBlock(
    buttonState: PttButtonState,
    speakerName: String?,
) {
    val colors = PttTheme.customColors
    val (eyebrow, color: Color, headline) =
        when (buttonState) {
            PttButtonState.Idle -> {
                Triple("Canal livre", colors.statusOnline, "Segure para falar")
            }

            PttButtonState.Requesting -> {
                Triple("Pedindo a palavra", colors.accentTx, "Aguardando…")
            }

            PttButtonState.Transmitting -> {
                Triple("Transmitindo", colors.accentTx, "Você está no ar")
            }

            PttButtonState.Receiving -> {
                Triple("Recebendo", MaterialTheme.colorScheme.primary, "${speakerName ?: "Alguém"} está falando")
            }
        }

    Column(
        modifier = Modifier.padding(horizontal = 24.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color = color, halo = buttonState != PttButtonState.Idle)
            SectionLabel(text = eyebrow, color = color)
        }
        Text(
            text = headline,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun PttButtonState.hint(): String =
    when (this) {
        PttButtonState.Idle -> "ou use o botão de mídia do fone ou do volante"
        PttButtonState.Requesting -> "solte para cancelar"
        PttButtonState.Transmitting -> "solte para encerrar"
        PttButtonState.Receiving -> "aguarde a sua vez para falar"
    }

@Composable
private fun ParticipantsPanel(
    state: PttState,
    buttonState: PttButtonState,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .glass(MaterialTheme.shapes.large)
                .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            SectionLabel(text = "No canal", modifier = Modifier.weight(1f))
            SectionLabel(text = state.participants.size.toString())
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        ) {
            items(state.participants, key = { it.userId }) { participant ->
                val isSelf = participant.userId == state.localUserId
                ParticipantAvatar(
                    name = participant.nickname,
                    isSpeaking = participant.userId == state.currentSpeakerId,
                    isRequesting = isSelf && buttonState == PttButtonState.Requesting,
                    isSelf = isSelf,
                )
            }
        }
    }
}

private val previewParticipants =
    listOf(
        ParticipantDto("u1", "Leandro", false),
        ParticipantDto("u2", "Marcos", true),
        ParticipantDto("u3", "Júlia", false),
        ParticipantDto("u4", "Rafael", false),
    )

@Preview
@Composable
private fun PttScreenPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            PttScreenContent(
                state =
                    PttState(
                        channelId = "Geral",
                        localUserId = "u1",
                        currentSpeakerId = "u2",
                        currentSpeakerName = "Marcos",
                        participants = previewParticipants,
                    ),
                onIntent = {},
            )
        }
    }
}

@Preview
@Composable
private fun PttScreenPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            PttScreenContent(
                state =
                    PttState(
                        channelId = "Geral",
                        localUserId = "u1",
                        isTransmitting = true,
                        isFloorGranted = true,
                        currentSpeakerId = "u1",
                        currentSpeakerName = "Leandro",
                        participants = previewParticipants,
                    ),
                onIntent = {},
            )
        }
    }
}
