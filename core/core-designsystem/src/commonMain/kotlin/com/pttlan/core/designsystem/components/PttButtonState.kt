package com.pttlan.core.designsystem.components

import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.ptt_action_start
import com.pttlan.core.designsystem.generated.resources.ptt_action_stop
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

/**
 * With a screen reader the button cannot be held, so a double tap toggles the talk (29.4): true when it starts
 * talking (also while someone else speaks, so the floor is asked for), false when it stops the talk or the request.
 */
fun PttButtonState.screenReaderStartsTalking(): Boolean = this == PttButtonState.Idle || this == PttButtonState.Receiving

/** What the screen reader says the double tap does, e.g. "double tap to start talking" (29.4). */
fun PttButtonState.screenReaderActionLabel(): StringResource =
    if (screenReaderStartsTalking()) Res.string.ptt_action_start else Res.string.ptt_action_stop
