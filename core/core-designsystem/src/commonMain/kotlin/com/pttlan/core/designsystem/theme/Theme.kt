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
    val iconOnAccentTx: Color,
    val iconOnPrimary: Color,
    val textTertiary: Color,
    val glassBase: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassStroke: Color,
    val glassHighlight: Color,
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
            iconOnAccentTx = Color.Unspecified,
            iconOnPrimary = Color.Unspecified,
            textTertiary = Color.Unspecified,
            glassBase = Color.Unspecified,
            glassTop = Color.Unspecified,
            glassBottom = Color.Unspecified,
            glassStroke = Color.Unspecified,
            glassHighlight = Color.Unspecified,
        )
    }

val LocalReduceTransparency = staticCompositionLocalOf { false }

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
        iconOnAccentTx = IconOnAccentTxDark,
        iconOnPrimary = IconOnPrimaryDark,
        textTertiary = TextTertiaryDark,
        glassBase = GlassBaseDark,
        glassTop = GlassTopDark,
        glassBottom = GlassBottomDark,
        glassStroke = GlassStrokeDark,
        glassHighlight = GlassHighlightDark,
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
        iconOnAccentTx = IconOnAccentTxLight,
        iconOnPrimary = IconOnPrimaryLight,
        textTertiary = TextTertiaryLight,
        glassBase = GlassBaseLight,
        glassTop = GlassTopLight,
        glassBottom = GlassBottomLight,
        glassStroke = GlassStrokeLight,
        glassHighlight = GlassHighlightLight,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = PrimaryDark,
        onPrimary = IconOnPrimaryDark,
        primaryContainer = PrimaryDimDark,
        onPrimaryContainer = TextPrimaryDark,
        background = BgDark,
        onBackground = TextPrimaryDark,
        surface = SurfaceDark,
        onSurface = TextPrimaryDark,
        surfaceVariant = Surface2Dark,
        onSurfaceVariant = TextSecondaryDark,
        surfaceContainerHigh = Surface2Dark,
        outline = BorderDark,
        outlineVariant = BorderDark,
        error = StatusOfflineDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = PrimaryLight,
        onPrimary = IconOnPrimaryLight,
        primaryContainer = PrimaryDimLight,
        onPrimaryContainer = IconOnPrimaryLight,
        background = BgLight,
        onBackground = TextPrimaryLight,
        surface = SurfaceLight,
        onSurface = TextPrimaryLight,
        surfaceVariant = Surface2Light,
        onSurfaceVariant = TextSecondaryLight,
        surfaceContainerHigh = SurfaceLight,
        outline = BorderLight,
        outlineVariant = BorderLight,
        error = StatusOfflineLight,
    )

object PttTheme {
    val customColors: PttCustomColors
        @Composable
        get() = LocalPttCustomColors.current

    /** When true, glass surfaces render as solid (user setting "Reduzir transparência"). */
    val reduceTransparency: Boolean
        @Composable
        get() = LocalReduceTransparency.current
}

@Composable
fun PttTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    reduceTransparency: Boolean = false,
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
        LocalReduceTransparency provides reduceTransparency,
    ) {
        MaterialTheme(
            colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
            typography = PttTypography,
            shapes = PttShapes,
            content = content,
        )
    }
}
