package com.pttlan.core.designsystem.components.snackbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

enum class PttSnackbarType(
    val icon: ImageVector? = null,
) {
    Success(
        icon = Icons.Default.CheckCircle,
    ),
    ErrorOrWarning(
        icon = Icons.Default.Warning,
    ),
    Generic,
}
