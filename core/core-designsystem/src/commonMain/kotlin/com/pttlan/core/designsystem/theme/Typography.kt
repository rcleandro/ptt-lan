package com.pttlan.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Using Default for UI and Monospace for Mono.
// If needed, custom fonts like IBM Plex Sans/Mono can be loaded here.

val PttTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold, // 700
                fontSize = 34.sp,
                letterSpacing = (-0.01).sp,
            ),
        headlineMedium =
            TextStyle( // Heading / H2
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold, // 600
                fontSize = 24.sp,
            ),
        bodyLarge =
            TextStyle( // Body
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal, // 400
                fontSize = 16.sp,
            ),
        bodySmall =
            TextStyle( // Caption
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal, // 400
                fontSize = 13.sp,
            ),
        labelMedium =
            TextStyle( // Mono / Técnico
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium, // 500
                fontSize = 14.sp,
            ),
    )
