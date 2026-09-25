package com.pttlan.feature.connection

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.certificate_changed_text
import com.pttlan.core.designsystem.generated.resources.certificate_changed_title
import com.pttlan.core.designsystem.generated.resources.certificate_trust
import com.pttlan.core.designsystem.generated.resources.common_cancel
import com.pttlan.core.designsystem.theme.PttTheme
import org.jetbrains.compose.resources.stringResource

/**
 * A LAN server showed another certificate than the one trusted before (30.5). The host sees the code of its own
 * certificate in the channel list, so the user can check it before trusting.
 */
@Composable
internal fun CertificateChangeDialog(
    change: CertificateChange,
    onTrust: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.certificate_changed_title)) },
        text = {
            Text(stringResource(Res.string.certificate_changed_text, change.previousCode, change.newCode))
        },
        shape = MaterialTheme.shapes.large,
        confirmButton = {
            TextButton(onClick = onTrust) {
                Text(stringResource(Res.string.certificate_trust), color = PttTheme.customColors.statusOffline)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
        },
    )
}
