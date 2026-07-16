@file:Suppress("DEPRECATION")

package com.pttlan.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme

@Preview
@Composable
fun PttButtonIdlePreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Surface {
            PttButton(state = PttButtonState.Idle, onPressStart = {}, onPressEnd = {})
        }
    }
}

@Preview
@Composable
fun PttButtonIdlePreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Surface {
            PttButton(state = PttButtonState.Idle, onPressStart = {}, onPressEnd = {})
        }
    }
}

@Preview
@Composable
fun PttButtonTransmittingPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Surface {
            PttButton(state = PttButtonState.Transmitting, onPressStart = {}, onPressEnd = {})
        }
    }
}

@Preview
@Composable
fun PttButtonTransmittingPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Surface {
            PttButton(state = PttButtonState.Transmitting, onPressStart = {}, onPressEnd = {})
        }
    }
}

@Preview
@Composable
fun PttButtonReceivingPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Surface {
            PttButton(state = PttButtonState.Receiving, onPressStart = {}, onPressEnd = {})
        }
    }
}

@Preview
@Composable
fun PttButtonReceivingPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Surface {
            PttButton(state = PttButtonState.Receiving, onPressStart = {}, onPressEnd = {})
        }
    }
}

@Preview
@Composable
fun ConnectionStatusBadgeOnlinePreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ConnectionStatusBadge(status = ConnectionStatus.Online)
            }
        }
    }
}

@Preview
@Composable
fun ConnectionStatusBadgeOnlinePreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ConnectionStatusBadge(status = ConnectionStatus.Online)
            }
        }
    }
}

@Preview
@Composable
fun ConnectionStatusBadgeReconnectingPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ConnectionStatusBadge(status = ConnectionStatus.Reconnecting)
            }
        }
    }
}

@Preview
@Composable
fun ConnectionStatusBadgeReconnectingPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ConnectionStatusBadge(status = ConnectionStatus.Reconnecting)
            }
        }
    }
}

@Preview
@Composable
fun ConnectionStatusBadgeOfflinePreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ConnectionStatusBadge(status = ConnectionStatus.Offline)
            }
        }
    }
}

@Preview
@Composable
fun ConnectionStatusBadgeOfflinePreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ConnectionStatusBadge(status = ConnectionStatus.Offline)
            }
        }
    }
}

@Preview
@Composable
fun ChannelCardPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
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
fun ChannelCardPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
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
fun ParticipantAvatarPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Surface {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ParticipantAvatar(name = "João Silva", isSpeaking = false)
                ParticipantAvatar(name = "Maria Souza", isSpeaking = true)
                ParticipantAvatar(name = "Admin", isSpeaking = false, showDetails = false)
            }
        }
    }
}

@Preview
@Composable
fun ParticipantAvatarPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Surface {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ParticipantAvatar(name = "João Silva", isSpeaking = false)
                ParticipantAvatar(name = "Maria Souza", isSpeaking = true)
                ParticipantAvatar(name = "Admin", isSpeaking = false, showDetails = false)
            }
        }
    }
}
