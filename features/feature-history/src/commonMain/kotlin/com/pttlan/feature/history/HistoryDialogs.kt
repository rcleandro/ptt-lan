package com.pttlan.feature.history

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.common_cancel
import com.pttlan.core.designsystem.theme.PttTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        shape = MaterialTheme.shapes.large,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text(confirmLabel, color = PttTheme.customColors.statusOffline)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
        },
    )
}
