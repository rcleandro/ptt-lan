package com.pttlan.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
}

@Immutable
data class PttCustomColors(
    val statusOnline: Color,
    val statusOnlineGlow: Color,
    val statusOffline: Color,
    val statusIdle: Color,
    val accentTx: Color,
    val accentTxGlow: Color,
    val primaryGlow: Color,
    val surface2: Color,
    val surface3: Color,
    val statusTransmitting: Color,
    val statusTransmittingGlow: Color,
    val iconOnAccentTx: Color,
    val iconOnPrimary: Color,
    val textTertiary: Color,
)

val LocalPttCustomColors =
    staticCompositionLocalOf {
        PttCustomColors(
            statusOnline = Color.Unspecified,
            statusOnlineGlow = Color.Unspecified,
            statusOffline = Color.Unspecified,
            statusIdle = Color.Unspecified,
            accentTx = Color.Unspecified,
            accentTxGlow = Color.Unspecified,
            primaryGlow = Color.Unspecified,
            surface2 = Color.Unspecified,
            surface3 = Color.Unspecified,
            statusTransmitting = Color.Unspecified,
            statusTransmittingGlow = Color.Unspecified,
            iconOnAccentTx = Color.Unspecified,
            iconOnPrimary = Color.Unspecified,
            textTertiary = Color.Unspecified,
        )
    }

private val DarkPttCustomColors =
    PttCustomColors(
        statusOnline = StatusOnlineDark,
        statusOnlineGlow = StatusOnlineGlowDark,
        statusOffline = StatusOfflineDark,
        statusIdle = StatusIdleDark,
        accentTx = AccentTxDark,
        accentTxGlow = AccentTxGlowDark,
        primaryGlow = PrimaryGlowDark,
        surface2 = Surface2Dark,
        surface3 = Surface3Dark,
        statusTransmitting = StatusTransmittingDark,
        statusTransmittingGlow = StatusTransmittingGlowDark,
        iconOnAccentTx = IconOnAccentTxDark,
        iconOnPrimary = IconOnPrimaryDark,
        textTertiary = TextTertiaryDark,
    )

private val LightPttCustomColors =
    PttCustomColors(
        statusOnline = StatusOnlineLight,
        statusOnlineGlow = StatusOnlineGlowLight,
        statusOffline = StatusOfflineLight,
        statusIdle = StatusIdleLight,
        accentTx = AccentTxLight,
        accentTxGlow = AccentTxGlowLight,
        primaryGlow = PrimaryGlowLight,
        surface2 = Surface2Light,
        surface3 = Surface3Light,
        statusTransmitting = StatusTransmittingLight,
        statusTransmittingGlow = StatusTransmittingGlowLight,
        iconOnAccentTx = IconOnAccentTxLight,
        iconOnPrimary = IconOnPrimaryLight,
        textTertiary = TextTertiaryLight,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = PrimaryDark,
        onPrimary = Color.White,
        primaryContainer = PrimaryDimDark,
        onPrimaryContainer = Color.White,
        background = BgDark,
        onBackground = TextPrimaryDark,
        surface = SurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = Surface2Dark,
        onSurfaceVariant = TextSecondaryDark,
        outline = BorderDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = PrimaryLight,
        onPrimary = Color.White,
        primaryContainer = PrimaryDimLight,
        onPrimaryContainer = Color.White,
        background = BgLight,
        onBackground = TextPrimaryLight,
        surface = SurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = Surface2Light,
        onSurfaceVariant = TextSecondaryLight,
        outline = BorderLight,
    )

object PttTheme {
    val customColors: PttCustomColors
        @Composable
        get() = LocalPttCustomColors.current
}

@Composable
fun PttTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark =
        when (appTheme) {
            AppTheme.SYSTEM -> isSystemInDarkTheme()
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
        }

    val customColors = if (isDark) DarkPttCustomColors else LightPttCustomColors

    CompositionLocalProvider(
        LocalPttCustomColors provides customColors,
    ) {
        MaterialTheme(
            colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
            typography = PttTypography,
            shapes = PttShapes,
            content = content,
        )
    }
}
