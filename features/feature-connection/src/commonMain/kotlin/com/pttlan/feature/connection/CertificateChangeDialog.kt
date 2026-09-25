package com.pttlan.feature.connection

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.pttlan.core.designsystem.theme.PttTheme

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
        title = { Text("O certificado deste servidor mudou") },
        text = {
            Text(
                "Antes: ${change.previousCode}\nAgora: ${change.newCode}\n\n" +
                    "Se quem hospeda reinstalou o app, confira com ele o código que aparece na lista de canais: " +
                    "sendo o novo, pode confiar. Se não, alguém na rede pode estar se passando pelo servidor.",
            )
        },
        shape = MaterialTheme.shapes.large,
        confirmButton = {
            TextButton(onClick = onTrust) {
                Text("Confiar no novo", color = PttTheme.customColors.statusOffline)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
