@file:Suppress("DEPRECATION")

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
import com.pttlan.core.designsystem.theme.PttTheme

@Preview
@Composable
fun PttButtonPreview() {
    PttTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PttButton(state = PttButtonState.Idle, onPressStart = {}, onPressEnd = {})
                PttButton(state = PttButtonState.Transmitting, onPressStart = {}, onPressEnd = {})
                PttButton(state = PttButtonState.Receiving, onPressStart = {}, onPressEnd = {})
            }
        }
    }
}

@Preview
@Composable
fun ConnectionStatusBadgePreview() {
    PttTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConnectionStatusBadge(status = ConnectionStatus.Online)
                ConnectionStatusBadge(status = ConnectionStatus.Reconnecting)
                ConnectionStatusBadge(status = ConnectionStatus.Offline)
            }
        }
    }
}

@Preview
@Composable
fun ChannelCardPreview() {
    PttTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChannelCard(
                    name = "Geral",
                    id = "geral-xyz",
                    participantCount = 42,
                    isActive = true,
                    onClick = {},
                )
                ChannelCard(
                    name = "Inativo",
                    id = "inativo-abc",
                    participantCount = 0,
                    isActive = false,
                    onClick = {},
                )
            }
        }
    }
}

@Preview
@Composable
fun ParticipantAvatarPreview() {
    PttTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ParticipantAvatar(name = "João Silva", isSpeaking = false)
                ParticipantAvatar(name = "Maria Souza", isSpeaking = true)
                ParticipantAvatar(name = "Admin", isSpeaking = false, showDetails = false)
            }
        }
    }
}
