package com.pttlan.wear

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import com.pttlan.core.designsystem.theme.AccentTxDark
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.core.designsystem.theme.BgDark
import com.pttlan.core.designsystem.theme.BorderDark
import com.pttlan.core.designsystem.theme.IbmPlexSans
import com.pttlan.core.designsystem.theme.IconOnAccentTxDark
import com.pttlan.core.designsystem.theme.IconOnPrimaryDark
import com.pttlan.core.designsystem.theme.PrimaryDark
import com.pttlan.core.designsystem.theme.PrimaryDimDark
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.core.designsystem.theme.StatusOfflineDark
import com.pttlan.core.designsystem.theme.SurfaceDark
import com.pttlan.core.designsystem.theme.TextPrimaryDark
import com.pttlan.core.designsystem.theme.TextSecondaryDark

/** The phone's dark palette on the watch's Material 3; watches only use dark themes, which suits OLED. */
private val WearPttColors =
    ColorScheme(
        primary = PrimaryDark,
        primaryDim = PrimaryDimDark,
        onPrimary = IconOnPrimaryDark,
        secondary = AccentTxDark,
        onSecondary = IconOnAccentTxDark,
        background = BgDark,
        onBackground = TextPrimaryDark,
        surfaceContainerLow = BgDark,
        surfaceContainer = SurfaceDark,
        surfaceContainerHigh = BorderDark,
        onSurface = TextPrimaryDark,
        onSurfaceVariant = TextSecondaryDark,
        outline = BorderDark,
        error = StatusOfflineDark,
    )

/**
 * Both themes: the watch's Material 3 for its own components, and PttTheme for the design system's, such as
 * the PTT button, so it looks and behaves as on the phone.
 */
@Composable
fun WearPttTheme(content: @Composable () -> Unit) {
    PttTheme(appTheme = AppTheme.DARK) {
        MaterialTheme(
            colorScheme = WearPttColors,
            typography = Typography(defaultFontFamily = IbmPlexSans),
            content = content,
        )
    }
}
