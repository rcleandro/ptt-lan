package com.pttlan.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Concentric radii (ADR 0006): inner radius = outer radius - padding.
// Controls are capsules (CircleShape); these cover containers. Named so the SwiftUI screens use the same ones (32.3).

/** Icon tiles inside cards. */
val IconTileRadius = 12.dp

/** Content cards. */
val CardRadius = 22.dp

/** Floating panels. */
val PanelRadius = 32.dp

/** Bottom dock (16dp padding around 48dp capsules). */
val DockRadius = 40.dp

val PttShapes =
    Shapes(
        small = RoundedCornerShape(IconTileRadius),
        medium = RoundedCornerShape(CardRadius),
        large = RoundedCornerShape(PanelRadius),
        extraLarge = RoundedCornerShape(DockRadius),
    )
