package com.pttlan.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme

@Composable
private fun PttButtonStatesSample() {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(48.dp)) {
        PttButtonState.entries.forEach { state ->
            PttButton(state = state, onPressStart = {}, onPressEnd = {}, buttonSize = 120.dp)
        }
    }
}

@Composable
private fun ControlsSample() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(24.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ConnectionStatus.entries.forEach { ConnectionStatusBadge(status = it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(text = "Conectar", onClick = {})
            PillButton(text = "Cancelar", onClick = {}, style = PillButtonStyle.Glass)
            PillButton(text = "Limpar", onClick = {}, style = PillButtonStyle.Destructive)
        }
        ChannelCard(name = "Geral", participantCount = 3, onClick = {})
        ChannelCard(name = "Equipe Evento", participantCount = 0, onClick = {})
        Row {
            ParticipantAvatar(name = "Júlia", isSpeaking = false)
            ParticipantAvatar(name = "Marcos", isSpeaking = true)
            ParticipantAvatar(name = "Leandro", isSpeaking = true, isSelf = true)
            ParticipantAvatar(name = "Leandro", isSpeaking = false, isRequesting = true, isSelf = true)
        }
    }
}

@Preview
@Composable
fun PttButtonPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) { Surface { PttButtonStatesSample() } }
}

@Preview
@Composable
fun PttButtonPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) { Surface { PttButtonStatesSample() } }
}

@Preview
@Composable
fun ControlsPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) { Surface { ControlsSample() } }
}

@Preview
@Composable
fun ControlsPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) { Surface { ControlsSample() } }
}

@Preview
@Composable
fun ControlsPreviewReducedTransparency() {
    PttTheme(appTheme = AppTheme.DARK, reduceTransparency = true) { Surface { ControlsSample() } }
}
