package com.pttlan.core.designsystem

import app.cash.paparazzi.Paparazzi
import com.pttlan.core.designsystem.components.PttButton
import com.pttlan.core.designsystem.components.PttButtonState
import com.pttlan.core.designsystem.components.ParticipantAvatar
import com.pttlan.core.designsystem.theme.PttTheme
import org.junit.Rule
import org.junit.Test
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

class PttButtonsSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi()

    @Test
    fun pttButtonStates() {
        paparazzi.snapshot {
            PttTheme {
                Column(
                    modifier = Modifier
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                ) {
                    PttButton(
                        state = PttButtonState.Idle,
                        onPressStart = {},
                        onPressEnd = {}
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PttButton(
                        state = PttButtonState.Transmitting,
                        onPressStart = {},
                        onPressEnd = {}
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PttButton(
                        state = PttButtonState.Receiving,
                        onPressStart = {},
                        onPressEnd = {}
                    )
                }
            }
        }
    }

    @Test
    fun participantAvatarStates() {
        paparazzi.snapshot {
            PttTheme {
                Column(
                    modifier = Modifier
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                ) {
                    ParticipantAvatar(
                        name = "Alice",
                        isSpeaking = false
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ParticipantAvatar(
                        name = "Bob",
                        isSpeaking = true
                    )
                }
            }
        }
    }
}
