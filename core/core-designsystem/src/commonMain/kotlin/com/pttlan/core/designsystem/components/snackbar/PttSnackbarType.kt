package com.pttlan.core.designsystem.components.snackbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pttlan.core.designsystem.theme.StatusOffline
import com.pttlan.core.designsystem.theme.StatusOnline
import com.pttlan.core.designsystem.theme.Surface2

enum class PttSnackbarType(
    val backgroundColor: Color,
    val icon: ImageVector? = null,
) {
    Success(
        backgroundColor = StatusOnline,
        icon = Icons.Default.CheckCircle,
    ),
    ErrorOrWarning(
        backgroundColor = StatusOffline,
        icon = Icons.Default.Warning,
    ),
    Generic(
        backgroundColor = Surface2,
    ),
}
