package com.pttlan.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.components.AmbientGlow
import com.pttlan.core.designsystem.components.GlassIconButton
import com.pttlan.core.designsystem.components.PillButton
import com.pttlan.core.designsystem.components.PillButtonStyle
import com.pttlan.core.designsystem.components.PttSwitch
import com.pttlan.core.designsystem.components.PttTopBar
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.SegmentedControl
import com.pttlan.core.designsystem.components.contentCard
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.PttTheme
import kotlin.math.roundToInt

private const val BYTES_PER_KB = 1024L
private const val MIN_CACHE_MB = 100f
private const val MAX_CACHE_MB = 2000f
private const val CACHE_SLIDER_STEPS = 18
private val TopBarClearance = 64.dp

private fun formatBytes(bytes: Long): String {
    val mb = BYTES_PER_KB * BYTES_PER_KB
    val gb = mb * BYTES_PER_KB
    return when {
        bytes >= gb -> "${(bytes.toDouble() / gb).roundToInt()} GB"
        bytes >= mb -> "${(bytes.toDouble() / mb).roundToInt()} MB"
        bytes >= BYTES_PER_KB -> "${(bytes.toDouble() / BYTES_PER_KB).roundToInt()} KB"
        else -> "$bytes B"
    }
}

@Composable
fun SettingsScreen(
    component: SettingsComponent,
    onBack: () -> Unit,
) {
    val state by component.state.collectAsState()

    SettingsScreenContent(
        state = state,
        onIntent = component::onIntent,
        onBack = onBack,
    )
}

@Composable
fun SettingsScreenContent(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    var showCacheLocationDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AmbientGlow(
            color = MaterialTheme.colorScheme.primary,
            intensity = 0.24f,
            modifier = Modifier.size(440.dp).align(Alignment.TopEnd).offset(180.dp, (-140).dp),
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 20.dp, end = 20.dp, top = TopBarClearance, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Configurações",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            AppearanceSection(state, onIntent)
            AudioSection(state, onIntent)
            HistorySection(
                state = state,
                onIntent = onIntent,
                onPickLocation = { showCacheLocationDialog = true },
                onClear = { showClearDialog = true },
            )
        }

        PttTopBar(navigation = { GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", onBack) })
    }

    if (showCacheLocationDialog) {
        CacheLocationDialog(state, onIntent, onDismiss = { showCacheLocationDialog = false })
    }
    if (showClearDialog) {
        ClearHistoryDialog(
            onConfirm = { onIntent(SettingsIntent.ClearCache) },
            onDismiss = { showClearDialog = false },
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionLabel(text = title, modifier = Modifier.padding(start = 16.dp, top = 10.dp))
    Column(modifier = Modifier.fillMaxWidth().contentCard(), content = content)
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        PttSwitch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun AppearanceSection(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    SettingsGroup(title = "Aparência") {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Tema", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            SegmentedControl(
                options = listOf(AppTheme.SYSTEM to "Sistema", AppTheme.LIGHT to "Claro", AppTheme.DARK to "Escuro"),
                selected = state.appTheme,
                onSelect = { onIntent(SettingsIntent.ChangeTheme(it)) },
            )
        }
        GroupDivider()
        SwitchRow(
            title = "Reduzir transparência",
            subtitle = "Troca o vidro por superfícies sólidas",
            checked = state.reduceTransparency,
            onCheckedChange = { onIntent(SettingsIntent.ToggleReduceTransparency(it)) },
        )
    }
}

@Composable
private fun AudioSection(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    SettingsGroup(title = "Áudio") {
        SwitchRow(
            title = "Codec Opus",
            subtitle = "Usa menos banda; recomendado fora da LAN",
            checked = state.useOpus,
            onCheckedChange = { onIntent(SettingsIntent.ToggleOpus(it)) },
        )
        GroupDivider()
        SwitchRow(
            title = "Microfone do fone Bluetooth",
            subtitle = "Fala pelo fone; o áudio fica com qualidade de telefone e o relógio pareado fica mudo. Vale na próxima conexão",
            checked = state.useHeadsetMic,
            onCheckedChange = { onIntent(SettingsIntent.ToggleHeadsetMic(it)) },
        )
        GroupDivider()
        SwitchRow(
            title = "Sempre ouvindo",
            subtitle = "Recebe áudio com o app em segundo plano",
            checked = state.alwaysListening,
            onCheckedChange = { onIntent(SettingsIntent.ToggleAlwaysListening(it)) },
        )
    }
}

@Composable
private fun HistorySection(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onPickLocation: () -> Unit,
    onClear: () -> Unit,
) {
    SettingsGroup(title = "Histórico") {
        SwitchRow(
            title = "Salvar áudios recebidos",
            checked = state.allowCache,
            onCheckedChange = { onIntent(SettingsIntent.ToggleAllowCache(it)) },
        )
        AnimatedVisibility(visible = state.allowCache, enter = expandVertically(), exit = shrinkVertically()) {
            Column {
                GroupDivider()
                LocationRow(state, onPickLocation)
                GroupDivider()
                CacheLimit(state, onIntent)
            }
        }
    }
    AnimatedVisibility(visible = state.allowCache, enter = expandVertically(), exit = shrinkVertically()) {
        PillButton(
            text = "Limpar histórico",
            onClick = onClear,
            style = PillButtonStyle.Destructive,
            icon = Icons.Default.Delete,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

@Composable
private fun LocationRow(
    state: SettingsState,
    onClick: () -> Unit,
) {
    val selected = state.storageOptions.find { it.id == state.cacheLocation }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Local",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            selected?.title ?: state.cacheLocation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PttTheme.customColors.textTertiary,
        )
    }
}

@Composable
private fun CacheLimit(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
) {
    val usage =
        if (state.maxCacheSizeMb == 0) 0f else (state.currentCacheUsageMb.toFloat() / state.maxCacheSizeMb).coerceIn(0f, 1f)

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Limite de espaço",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text("${state.maxCacheSizeMb} MB", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = state.maxCacheSizeMb.toFloat(),
            onValueChange = { onIntent(SettingsIntent.ChangeMaxCacheSize(it.roundToInt())) },
            valueRange = MIN_CACHE_MB..MAX_CACHE_MB,
            steps = CACHE_SLIDER_STEPS,
            colors =
                SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = PttTheme.customColors.surface3,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                ),
        )
        LinearProgressIndicator(
            progress = { usage },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = PttTheme.customColors.surface3,
            drawStopIndicator = {},
        )
        Spacer(modifier = Modifier.height(6.dp))
        SectionLabel(text = "${state.currentCacheUsageMb} MB de ${state.maxCacheSizeMb} MB usados")
    }
}

@Composable
private fun CacheLocationDialog(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Local de armazenamento") },
        shape = MaterialTheme.shapes.large,
        text = {
            Column {
                state.storageOptions.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = option.id == state.cacheLocation,
                                onClick = {
                                    onIntent(SettingsIntent.ChangeCacheLocation(option.id))
                                    onDismiss()
                                },
                                role = Role.RadioButton,
                            ).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option.id == state.cacheLocation, onClick = null)
                        Text(
                            text = "${option.title} (${formatBytes(option.availableSpaceBytes)} livre)",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
                if (state.isExternalStorageSupported && state.storageOptions.size == 1) {
                    Text(
                        text = "O armazenamento SD não está disponível. Insira um cartão SD para usar essa opção.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PttTheme.customColors.statusOffline,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
private fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Limpar histórico") },
        text = { Text("Tem certeza que deseja apagar todos os áudios gravados? Esta ação não pode ser desfeita.") },
        shape = MaterialTheme.shapes.large,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text("Limpar", color = PttTheme.customColors.statusOffline)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Preview
@Composable
private fun SettingsScreenPreviewDark() {
    PttTheme(appTheme = AppTheme.DARK) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            SettingsScreenContent(
                state = SettingsState(useOpus = true, appTheme = AppTheme.DARK, allowCache = true, currentCacheUsageMb = 128),
                onIntent = {},
            )
        }
    }
}

@Preview
@Composable
private fun SettingsScreenPreviewLight() {
    PttTheme(appTheme = AppTheme.LIGHT) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) {
            SettingsScreenContent(
                state = SettingsState(useOpus = true, appTheme = AppTheme.LIGHT),
                onIntent = {},
            )
        }
    }
}
