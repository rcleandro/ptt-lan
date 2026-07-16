package com.pttlan.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme

@Composable
fun SettingsScreen(component: SettingsComponent) {
    val state by component.state.collectAsState()

    SettingsScreenContent(
        state = state,
        onIntent = component::onIntent,
    )
}

@Composable
fun SettingsScreenContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .then(modifier),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text("Usar codec Opus", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Compressão avançada de áudio. Reduz consumo de rede significativamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.useOpus,
                onCheckedChange = { onIntent(SettingsIntent.ToggleOpus(it)) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text("Sempre Ouvindo", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Receber áudio mesmo quando o app estiver em segundo plano (requer permissão especial).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.alwaysListening,
                onCheckedChange = { onIntent(SettingsIntent.ToggleAlwaysListening(it)) },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true }
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text("Tema", style = MaterialTheme.typography.titleMedium)
                Text(
                    when (state.appTheme) {
                        AppTheme.SYSTEM -> "Automático (sistema)"
                        AppTheme.LIGHT -> "Claro"
                        AppTheme.DARK -> "Escuro"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(text = "Escolha um tema")
            },
            text = {
                Column {
                    val themeOptions =
                        listOf(
                            AppTheme.SYSTEM to "Automático (sistema)",
                            AppTheme.LIGHT to "Claro",
                            AppTheme.DARK to "Escuro",
                        )
                    themeOptions.forEach { (theme, title) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (theme == state.appTheme),
                                    onClick = {
                                        onIntent(SettingsIntent.ChangeTheme(theme))
                                        showThemeDialog = false
                                    },
                                    role = Role.RadioButton,
                                ).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (theme == state.appTheme),
                                onClick = null, // null recommended for accessibility with selectable
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Surface {
            SettingsScreenContent(
                state =
                    SettingsState(
                        nickname = "Leandro",
                        useOpus = true,
                        appTheme = AppTheme.DARK,
                        alwaysListening = false,
                    ),
                onIntent = {},
            )
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Surface {
            SettingsScreenContent(
                state =
                    SettingsState(
                        nickname = "Leandro",
                        useOpus = true,
                        appTheme = AppTheme.LIGHT,
                        alwaysListening = false,
                    ),
                onIntent = {},
            )
        }
    }
}
