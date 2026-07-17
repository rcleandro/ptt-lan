package com.pttlan.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import kotlin.math.roundToInt

private fun formatBytes(bytes: Long): String {
    val kb = 1024L
    val mb = kb * 1024L
    val gb = mb * 1024L
    return when {
        bytes >= gb -> "${(bytes.toDouble() / gb).roundToInt()} GB"
        bytes >= mb -> "${(bytes.toDouble() / mb).roundToInt()} MB"
        bytes >= kb -> "${(bytes.toDouble() / kb).roundToInt()} KB"
        else -> "$bytes B"
    }
}

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
    var showCacheLocationDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .then(modifier),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true }
                    .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp),
            )
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

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp),
            )
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

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = Icons.Default.Headset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp),
            )
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

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp),
            )
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text("Permitir histórico de audios", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Salvar histórico localmente para reduzir consumo de rede no futuro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.allowCache,
                onCheckedChange = { onIntent(SettingsIntent.ToggleAllowCache(it)) },
            )
        }

        AnimatedVisibility(
            visible = state.allowCache,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { showCacheLocationDialog = true }
                            .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Icon(
                        imageVector = Icons.Default.SdStorage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                        Text("Local de armazenamento", style = MaterialTheme.typography.titleMedium)

                        val selectedOption = state.storageOptions.find { it.id == state.cacheLocation }
                        val displayLocation = selectedOption?.title ?: state.cacheLocation
                        Text(
                            displayLocation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Tamanho total permitido",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${state.maxCacheSizeMb} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = state.maxCacheSizeMb.toFloat(),
                        onValueChange = { onIntent(SettingsIntent.ChangeMaxCacheSize(it.roundToInt())) },
                        valueRange = 100f..2000f,
                        steps = 19,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Armazenado",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "${state.currentCacheUsageMb} MB de ${state.maxCacheSizeMb} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = {
                            if (state.maxCacheSizeMb == 0) {
                                0f
                            } else {
                                (state.currentCacheUsageMb.toFloat() / state.maxCacheSizeMb).coerceIn(0f, 1f)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp).size(20.dp),
                    )
                    Text("Limpar histórico")
                }
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

    if (showCacheLocationDialog) {
        AlertDialog(
            onDismissRequest = { showCacheLocationDialog = false },
            title = {
                Text(text = "Local de armazenamento")
            },
            text = {
                Column {
                    state.storageOptions.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (option.id == state.cacheLocation),
                                    onClick = {
                                        onIntent(SettingsIntent.ChangeCacheLocation(option.id))
                                        showCacheLocationDialog = false
                                    },
                                    role = Role.RadioButton,
                                ).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (option.id == state.cacheLocation),
                                onClick = null,
                            )
                            Text(
                                text = "${option.title} (${formatBytes(option.availableSpaceBytes)} Livre)",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }

                    if (state.isExternalStorageSupported && state.storageOptions.size == 1) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text =
                                "O armazenamento SD não está disponível. Por favor, insira um cartão SD para poder usar essa funcionalidade.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCacheLocationDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCacheLocationDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Limpar Histórico") },
            text = { Text("Tem certeza que deseja apagar todos os áudios gravados? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onIntent(SettingsIntent.ClearCache)
                    },
                ) {
                    Text("Limpar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
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
