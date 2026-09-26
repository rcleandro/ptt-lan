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
import com.pttlan.core.common.storage.STORAGE_EXTERNAL
import com.pttlan.core.designsystem.components.AmbientGlow
import com.pttlan.core.designsystem.components.GlassIconButton
import com.pttlan.core.designsystem.components.PillButton
import com.pttlan.core.designsystem.components.PillButtonStyle
import com.pttlan.core.designsystem.components.PttSwitch
import com.pttlan.core.designsystem.components.PttTopBar
import com.pttlan.core.designsystem.components.SectionLabel
import com.pttlan.core.designsystem.components.SegmentedControl
import com.pttlan.core.designsystem.components.contentCard
import com.pttlan.core.designsystem.components.readableWidth
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.common_cancel
import com.pttlan.core.designsystem.generated.resources.common_ok
import com.pttlan.core.designsystem.generated.resources.settings_always_listening
import com.pttlan.core.designsystem.generated.resources.settings_always_listening_detail
import com.pttlan.core.designsystem.generated.resources.settings_appearance
import com.pttlan.core.designsystem.generated.resources.settings_audio
import com.pttlan.core.designsystem.generated.resources.settings_back
import com.pttlan.core.designsystem.generated.resources.settings_clear_confirm
import com.pttlan.core.designsystem.generated.resources.settings_clear_history
import com.pttlan.core.designsystem.generated.resources.settings_clear_history_text
import com.pttlan.core.designsystem.generated.resources.settings_headset_mic
import com.pttlan.core.designsystem.generated.resources.settings_headset_mic_detail
import com.pttlan.core.designsystem.generated.resources.settings_history
import com.pttlan.core.designsystem.generated.resources.settings_location
import com.pttlan.core.designsystem.generated.resources.settings_location_option
import com.pttlan.core.designsystem.generated.resources.settings_location_title
import com.pttlan.core.designsystem.generated.resources.settings_opus
import com.pttlan.core.designsystem.generated.resources.settings_opus_detail
import com.pttlan.core.designsystem.generated.resources.settings_reduce_transparency
import com.pttlan.core.designsystem.generated.resources.settings_reduce_transparency_detail
import com.pttlan.core.designsystem.generated.resources.settings_save_audio
import com.pttlan.core.designsystem.generated.resources.settings_sd_unavailable
import com.pttlan.core.designsystem.generated.resources.settings_size_mb
import com.pttlan.core.designsystem.generated.resources.settings_space_limit
import com.pttlan.core.designsystem.generated.resources.settings_storage_external
import com.pttlan.core.designsystem.generated.resources.settings_storage_internal
import com.pttlan.core.designsystem.generated.resources.settings_theme
import com.pttlan.core.designsystem.generated.resources.settings_theme_dark
import com.pttlan.core.designsystem.generated.resources.settings_theme_light
import com.pttlan.core.designsystem.generated.resources.settings_theme_system
import com.pttlan.core.designsystem.generated.resources.settings_title
import com.pttlan.core.designsystem.generated.resources.settings_usage
import com.pttlan.core.designsystem.generated.resources.size_bytes
import com.pttlan.core.designsystem.generated.resources.size_gb
import com.pttlan.core.designsystem.generated.resources.size_kb
import com.pttlan.core.designsystem.generated.resources.size_mb
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private const val BYTES_PER_KB = 1024L
private const val MIN_CACHE_MB = 100f
private const val MAX_CACHE_MB = 2000f
private const val CACHE_SLIDER_STEPS = 18
private val TopBarClearance = 64.dp

private const val GLOW_INTENSITY = 0.24f
private val GlowSize = 440.dp
private val GlowOffsetX = 180.dp
private val GlowOffsetY = (-140).dp
private val SettingsSpacing = 10.dp
private val UsageBarHeight = 6.dp
private val StorageOptionHeight = 56.dp

@Composable
private fun formatBytes(bytes: Long): String {
    val mb = BYTES_PER_KB * BYTES_PER_KB
    val gb = mb * BYTES_PER_KB
    return when {
        bytes >= gb -> stringResource(Res.string.size_gb, (bytes.toDouble() / gb).roundToInt())
        bytes >= mb -> stringResource(Res.string.size_mb, (bytes.toDouble() / mb).roundToInt())
        bytes >= BYTES_PER_KB -> stringResource(Res.string.size_kb, (bytes.toDouble() / BYTES_PER_KB).roundToInt())
        else -> stringResource(Res.string.size_bytes, bytes)
    }
}

/** The name of a storage place, from its id. */
@Composable
private fun storageName(id: String): String =
    stringResource(if (id == STORAGE_EXTERNAL) Res.string.settings_storage_external else Res.string.settings_storage_internal)

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
            intensity = GLOW_INTENSITY,
            modifier = Modifier.size(GlowSize).align(Alignment.TopEnd).offset(GlowOffsetX, GlowOffsetY),
        )

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .readableWidth()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = Dimens.Space2xl, end = Dimens.Space2xl, top = TopBarClearance, bottom = Dimens.Space3xl),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing),
        ) {
            Text(
                text = stringResource(Res.string.settings_title),
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

        PttTopBar(
            modifier = Modifier.readableWidth(),
            navigation = { GlassIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.settings_back), onBack) },
        )
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
    SectionLabel(text = title, modifier = Modifier.padding(start = Dimens.SpaceXl, top = SettingsSpacing))
    Column(modifier = Modifier.fillMaxWidth().contentCard(), content = content)
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = Dimens.SpaceXl), color = MaterialTheme.colorScheme.outline)
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
                .padding(horizontal = Dimens.SpaceXl, vertical = Dimens.SpaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
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
    SettingsGroup(title = stringResource(Res.string.settings_appearance)) {
        Column(
            modifier = Modifier.padding(horizontal = Dimens.SpaceXl, vertical = Dimens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing),
        ) {
            Text(
                stringResource(Res.string.settings_theme),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            SegmentedControl(
                options =
                    listOf(
                        AppTheme.SYSTEM to stringResource(Res.string.settings_theme_system),
                        AppTheme.LIGHT to stringResource(Res.string.settings_theme_light),
                        AppTheme.DARK to stringResource(Res.string.settings_theme_dark),
                    ),
                selected = state.appTheme,
                onSelect = { onIntent(SettingsIntent.ChangeTheme(it)) },
            )
        }
        GroupDivider()
        SwitchRow(
            title = stringResource(Res.string.settings_reduce_transparency),
            subtitle = stringResource(Res.string.settings_reduce_transparency_detail),
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
    SettingsGroup(title = stringResource(Res.string.settings_audio)) {
        SwitchRow(
            title = stringResource(Res.string.settings_opus),
            subtitle = stringResource(Res.string.settings_opus_detail),
            checked = state.useOpus,
            onCheckedChange = { onIntent(SettingsIntent.ToggleOpus(it)) },
        )
        GroupDivider()
        SwitchRow(
            title = stringResource(Res.string.settings_headset_mic),
            subtitle = stringResource(Res.string.settings_headset_mic_detail),
            checked = state.useHeadsetMic,
            onCheckedChange = { onIntent(SettingsIntent.ToggleHeadsetMic(it)) },
        )
        GroupDivider()
        SwitchRow(
            title = stringResource(Res.string.settings_always_listening),
            subtitle = stringResource(Res.string.settings_always_listening_detail),
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
    SettingsGroup(title = stringResource(Res.string.settings_history)) {
        SwitchRow(
            title = stringResource(Res.string.settings_save_audio),
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
            text = stringResource(Res.string.settings_clear_history),
            onClick = onClear,
            style = PillButtonStyle.Destructive,
            icon = Icons.Default.Delete,
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpaceSm),
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Dimens.SpaceXl, vertical = Dimens.SpaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Text(
            stringResource(Res.string.settings_location),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            storageName(selected?.id ?: state.cacheLocation),
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

    Column(modifier = Modifier.padding(horizontal = Dimens.SpaceXl, vertical = Dimens.SpaceLg)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.settings_space_limit),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.settings_size_mb, state.maxCacheSizeMb),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        CacheSizeSlider(state.maxCacheSizeMb, onIntent)
        LinearProgressIndicator(
            progress = { usage },
            modifier = Modifier.fillMaxWidth().height(UsageBarHeight).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = PttTheme.customColors.surface3,
            drawStopIndicator = {},
        )
        Spacer(modifier = Modifier.height(UsageBarHeight))
        SectionLabel(text = stringResource(Res.string.settings_usage, state.currentCacheUsageMb, state.maxCacheSizeMb))
    }
}

@Composable
private fun CacheSizeSlider(
    maxCacheSizeMb: Int,
    onIntent: (SettingsIntent) -> Unit,
) {
    Slider(
        value = maxCacheSizeMb.toFloat(),
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
}

@Composable
private fun CacheLocationDialog(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.settings_location_title)) },
        shape = MaterialTheme.shapes.large,
        text = {
            Column {
                state.storageOptions.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(StorageOptionHeight)
                            .selectable(
                                selected = option.id == state.cacheLocation,
                                onClick = {
                                    onIntent(SettingsIntent.ChangeCacheLocation(option.id))
                                    onDismiss()
                                },
                                role = Role.RadioButton,
                            ).padding(horizontal = Dimens.SpaceXl),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option.id == state.cacheLocation, onClick = null)
                        Text(
                            text =
                                stringResource(
                                    Res.string.settings_location_option,
                                    storageName(option.id),
                                    formatBytes(option.availableSpaceBytes),
                                ),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = Dimens.SpaceXl),
                        )
                    }
                }
                if (state.isExternalStorageSupported && state.storageOptions.size == 1) {
                    Text(
                        text = stringResource(Res.string.settings_sd_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = PttTheme.customColors.statusOffline,
                        modifier = Modifier.padding(horizontal = Dimens.SpaceXl, vertical = Dimens.SpaceXl),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_ok)) } },
    )
}

@Composable
private fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_clear_history)) },
        text = { Text(stringResource(Res.string.settings_clear_history_text)) },
        shape = MaterialTheme.shapes.large,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text(stringResource(Res.string.settings_clear_confirm), color = PttTheme.customColors.statusOffline)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) } },
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
