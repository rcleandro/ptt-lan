package com.pttlan.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing shared by every screen: the Compose Multiplatform stand-in for Android's `dimens.xml`. A margin, padding
 * or gap comes from here instead of an inline `dp`, so the rhythm of the app changes in one place. A size that
 * belongs to a single component (an icon, an avatar) stays a named constant in that component's file.
 */
object Dimens {
    /** Borders and dividers. */
    val Hairline = 1.dp

    val Space2xs = 2.dp
    val SpaceXs = 4.dp
    val SpaceSm = 6.dp
    val SpaceMd = 8.dp
    val SpaceLg = 12.dp
    val SpaceXl = 16.dp
    val Space2xl = 20.dp
    val Space3xl = 24.dp
    val Space4xl = 32.dp

    /** Round glass controls, toolbars and the status badge. */
    val GlassControl = 44.dp

    /** Smallest touch area a control gets, whatever it draws. */
    val TouchTarget = 48.dp
}
