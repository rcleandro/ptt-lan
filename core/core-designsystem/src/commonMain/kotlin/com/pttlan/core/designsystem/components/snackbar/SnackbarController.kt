package com.pttlan.core.designsystem.components.snackbar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pttlan.core.designsystem.theme.Surface2
import com.pttlan.core.designsystem.theme.TextPrimary
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

data class SnackbarEvent(
    val message: String = "",
    val containerColor: Color = Surface2,
    val contentColor: Color = TextPrimary,
    val type: PttSnackbarType = PttSnackbarType.Generic,
    val icon: ImageVector? = null,
)

object SnackbarController {
    private val _events = Channel<SnackbarEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    suspend fun sendEvent(event: SnackbarEvent) {
        _events.send(event)
    }
}
