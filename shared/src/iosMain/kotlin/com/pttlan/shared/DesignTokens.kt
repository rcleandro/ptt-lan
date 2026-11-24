package com.pttlan.shared

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import com.pttlan.core.designsystem.components.ChannelPaneWidth
import com.pttlan.core.designsystem.components.ExpandedWidth
import com.pttlan.core.designsystem.components.ReadableWidth
import com.pttlan.core.designsystem.theme.AccentTxDark
import com.pttlan.core.designsystem.theme.AccentTxGlowDark
import com.pttlan.core.designsystem.theme.AccentTxGlowLight
import com.pttlan.core.designsystem.theme.AccentTxLight
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.BgDark
import com.pttlan.core.designsystem.theme.BgLight
import com.pttlan.core.designsystem.theme.BorderDark
import com.pttlan.core.designsystem.theme.BorderLight
import com.pttlan.core.designsystem.theme.CardRadius
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.DockRadius
import com.pttlan.core.designsystem.theme.IconOnAccentTxDark
import com.pttlan.core.designsystem.theme.IconOnAccentTxLight
import com.pttlan.core.designsystem.theme.IconOnPrimaryDark
import com.pttlan.core.designsystem.theme.IconOnPrimaryLight
import com.pttlan.core.designsystem.theme.PanelRadius
import com.pttlan.core.designsystem.theme.PrimaryDark
import com.pttlan.core.designsystem.theme.PrimaryGlowDark
import com.pttlan.core.designsystem.theme.PrimaryGlowLight
import com.pttlan.core.designsystem.theme.PrimaryLight
import com.pttlan.core.designsystem.theme.StatusIdleDark
import com.pttlan.core.designsystem.theme.StatusIdleLight
import com.pttlan.core.designsystem.theme.StatusOfflineDark
import com.pttlan.core.designsystem.theme.StatusOfflineLight
import com.pttlan.core.designsystem.theme.StatusOnlineDark
import com.pttlan.core.designsystem.theme.StatusOnlineLight
import com.pttlan.core.designsystem.theme.SurfaceDark
import com.pttlan.core.designsystem.theme.SurfaceLight
import com.pttlan.core.designsystem.theme.TextPrimaryDark
import com.pttlan.core.designsystem.theme.TextPrimaryLight
import com.pttlan.core.designsystem.theme.TextSecondaryDark
import com.pttlan.core.designsystem.theme.TextSecondaryLight
import com.pttlan.core.designsystem.theme.TextTertiaryDark
import com.pttlan.core.designsystem.theme.TextTertiaryLight

/** Design-system colors for the SwiftUI screens, as ARGB, from the same tokens as Compose (ADR 0006, 32.3). */
class SwiftPalette internal constructor(
    val background: Int,
    val surface: Int,
    val border: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val textTertiary: Int,
    val primary: Int,
    val primaryGlow: Int,
    val onPrimary: Int,
    val accentTx: Int,
    val accentTxGlow: Int,
    val onAccentTx: Int,
    val statusOnline: Int,
    val statusOffline: Int,
    val statusIdle: Int,
)

private val DarkPalette =
    SwiftPalette(
        background = BgDark.toArgb(),
        surface = SurfaceDark.toArgb(),
        border = BorderDark.toArgb(),
        textPrimary = TextPrimaryDark.toArgb(),
        textSecondary = TextSecondaryDark.toArgb(),
        textTertiary = TextTertiaryDark.toArgb(),
        primary = PrimaryDark.toArgb(),
        primaryGlow = PrimaryGlowDark.toArgb(),
        onPrimary = IconOnPrimaryDark.toArgb(),
        accentTx = AccentTxDark.toArgb(),
        accentTxGlow = AccentTxGlowDark.toArgb(),
        onAccentTx = IconOnAccentTxDark.toArgb(),
        statusOnline = StatusOnlineDark.toArgb(),
        statusOffline = StatusOfflineDark.toArgb(),
        statusIdle = StatusIdleDark.toArgb(),
    )

private val LightPalette =
    SwiftPalette(
        background = BgLight.toArgb(),
        surface = SurfaceLight.toArgb(),
        border = BorderLight.toArgb(),
        textPrimary = TextPrimaryLight.toArgb(),
        textSecondary = TextSecondaryLight.toArgb(),
        textTertiary = TextTertiaryLight.toArgb(),
        primary = PrimaryLight.toArgb(),
        primaryGlow = PrimaryGlowLight.toArgb(),
        onPrimary = IconOnPrimaryLight.toArgb(),
        accentTx = AccentTxLight.toArgb(),
        accentTxGlow = AccentTxGlowLight.toArgb(),
        onAccentTx = IconOnAccentTxLight.toArgb(),
        statusOnline = StatusOnlineLight.toArgb(),
        statusOffline = StatusOfflineLight.toArgb(),
        statusIdle = StatusIdleLight.toArgb(),
    )

fun swiftPalette(dark: Boolean): SwiftPalette = if (dark) DarkPalette else LightPalette

/** Whether [theme] (the user's choice in settings) draws dark, given the system's appearance. */
fun isDarkTheme(
    theme: AppTheme,
    systemIsDark: Boolean,
): Boolean =
    when (theme) {
        AppTheme.SYSTEM -> systemIsDark
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

/** Radii and spacings in points, from `Shape.kt` and [Dimens]. */
object SwiftDimens {
    val cardRadius: Double = CardRadius.points
    val panelRadius: Double = PanelRadius.points
    val dockRadius: Double = DockRadius.points
    val spaceXs: Double = Dimens.SpaceXs.points
    val spaceSm: Double = Dimens.SpaceSm.points
    val spaceMd: Double = Dimens.SpaceMd.points
    val spaceLg: Double = Dimens.SpaceLg.points
    val spaceXl: Double = Dimens.SpaceXl.points
    val space2xl: Double = Dimens.Space2xl.points
    val glassControl: Double = Dimens.GlassControl.points
    val touchTarget: Double = Dimens.TouchTarget.points
    val expandedWidth: Double = ExpandedWidth.points
    val channelPaneWidth: Double = ChannelPaneWidth.points
    val readableWidth: Double = ReadableWidth.points
}

/** One dp is one point on iOS. */
private val Dp.points: Double get() = value.toDouble()
