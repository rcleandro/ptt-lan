package com.pttlan.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.PttTheme

private const val DISABLED_ALPHA = 0.45f
private const val DESTRUCTIVE_FILL_ALPHA = 0.14f

/** 44dp round glass button for top bars. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground,
) {
    Box(
        modifier =
            modifier
                .size(44.dp)
                .glass(CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

enum class PillButtonStyle {
    Primary,
    Glass,
    GlassDestructive,
    Destructive,
}

/** 48dp capsule button. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillButtonStyle = PillButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colors = PttTheme.customColors
    val contentColor =
        when (style) {
            PillButtonStyle.Primary -> MaterialTheme.colorScheme.onPrimary
            PillButtonStyle.Glass -> MaterialTheme.colorScheme.onBackground
            PillButtonStyle.GlassDestructive, PillButtonStyle.Destructive -> colors.statusOffline
        }
    val container =
        when (style) {
            PillButtonStyle.Primary -> {
                Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
            }

            PillButtonStyle.Glass, PillButtonStyle.GlassDestructive -> {
                Modifier.glass(CircleShape)
            }

            PillButtonStyle.Destructive -> {
                Modifier.clip(CircleShape).background(colors.statusOffline.copy(alpha = DESTRUCTIVE_FILL_ALPHA))
            }
        }

    Row(
        modifier =
            modifier
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .then(container)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .heightIn(min = 48.dp)
                .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

/** 48dp capsule single-line input for docks. */
@Composable
fun PttTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val baseStyle = if (monospace) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyLarge
    val textStyle = baseStyle.copy(color = MaterialTheme.colorScheme.onBackground)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.semantics { contentDescription = placeholder },
        decorationBox = { innerTextField ->
            Box(
                modifier =
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(text = placeholder, style = textStyle, color = PttTheme.customColors.textTertiary)
                }
                innerTextField()
            }
        },
    )
}

/** Borderless input inside a content card, with a small mono label on top. */
@Composable
fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .contentCard()
                .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SectionLabel(text = label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        )
    }
}

@Composable
fun PttSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors =
            SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PttTheme.customColors.statusOnline,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = PttTheme.customColors.surface3,
                uncheckedBorderColor = Color.Transparent,
            ),
    )
}

/** Capsule segmented control; the selected segment is a glass thumb. */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .padding(4.dp)
                .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .then(if (isSelected) Modifier.glass(CircleShape) else Modifier.clip(CircleShape))
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}
