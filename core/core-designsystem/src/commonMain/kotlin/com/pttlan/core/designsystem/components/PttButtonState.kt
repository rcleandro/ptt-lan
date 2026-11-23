package com.pttlan.core.designsystem.components

import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.ptt_button_idle
import com.pttlan.core.designsystem.generated.resources.ptt_button_receiving
import com.pttlan.core.designsystem.generated.resources.ptt_button_requesting
import com.pttlan.core.designsystem.generated.resources.ptt_button_transmitting
import org.jetbrains.compose.resources.StringResource

enum class PttButtonState {
    Idle,
    Requesting,
    Transmitting,
    Receiving,
}

/** What a screen reader says for the button in each state; shared by the Compose and SwiftUI buttons (32.4). */
fun PttButtonState.accessibilityLabel(): StringResource =
    when (this) {
        PttButtonState.Idle -> Res.string.ptt_button_idle
        PttButtonState.Requesting -> Res.string.ptt_button_requesting
        PttButtonState.Transmitting -> Res.string.ptt_button_transmitting
        PttButtonState.Receiving -> Res.string.ptt_button_receiving
    }
