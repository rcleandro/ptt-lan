package com.pttlan.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Concentric radii (ADR 0006): inner radius = outer radius - padding.
// Controls are capsules (CircleShape); these cover containers.
val PttShapes =
    Shapes(
        small = RoundedCornerShape(12.dp), // icon tiles inside cards
        medium = RoundedCornerShape(22.dp), // content cards
        large = RoundedCornerShape(32.dp), // floating panels
        extraLarge = RoundedCornerShape(40.dp), // bottom dock (16dp padding around 48dp capsules)
    )
