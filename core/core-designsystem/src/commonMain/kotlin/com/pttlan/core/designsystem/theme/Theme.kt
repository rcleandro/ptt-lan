package com.pttlan.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class PttCustomColors(
    val statusOnline: Color,
    val statusOffline: Color,
    val statusIdle: Color,
    val accentTx: Color,
    val accentTxGlow: Color,
    val primaryGlow: Color,
    val surface2: Color,
    val surface3: Color,
    val textTertiary: Color,
)

val LocalPttCustomColors =
    staticCompositionLocalOf {
        PttCustomColors(
            statusOnline = Color.Unspecified,
            statusOffline = Color.Unspecified,
            statusIdle = Color.Unspecified,
            accentTx = Color.Unspecified,
            accentTxGlow = Color.Unspecified,
            primaryGlow = Color.Unspecified,
            surface2 = Color.Unspecified,
            surface3 = Color.Unspecified,
            textTertiary = Color.Unspecified,
        )
    }

private val DefaultPttCustomColors =
    PttCustomColors(
        statusOnline = StatusOnline,
        statusOffline = StatusOffline,
        statusIdle = StatusIdle,
        accentTx = AccentTx,
        accentTxGlow = AccentTxGlow,
        primaryGlow = PrimaryGlow,
        surface2 = Surface2,
        surface3 = Surface3,
        textTertiary = TextTertiary,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Primary,
        onPrimary = Color.White,
        primaryContainer = PrimaryDim,
        onPrimaryContainer = Color.White,
        background = Bg,
        onBackground = TextPrimary,
        surface = Surface,
        onSurface = TextPrimary,
        surfaceVariant = Surface2,
        onSurfaceVariant = TextSecondary,
        outline = Border,
    )

object PttTheme {
    val customColors: PttCustomColors
        @Composable
        get() = LocalPttCustomColors.current
}

@Composable
fun PttTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalPttCustomColors provides DefaultPttCustomColors,
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = PttTypography,
            shapes = PttShapes,
            content = content,
        )
    }
}
