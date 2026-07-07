package com.pttlan.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.ibm_plex_mono_medium
import com.pttlan.core.designsystem.generated.resources.ibm_plex_mono_regular
import com.pttlan.core.designsystem.generated.resources.ibm_plex_sans_bold
import com.pttlan.core.designsystem.generated.resources.ibm_plex_sans_medium
import com.pttlan.core.designsystem.generated.resources.ibm_plex_sans_regular
import com.pttlan.core.designsystem.generated.resources.ibm_plex_sans_semibold
import org.jetbrains.compose.resources.Font

val IbmPlexSans
    @Composable get() =
        FontFamily(
            Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal),
            Font(Res.font.ibm_plex_sans_medium, FontWeight.Medium),
            Font(Res.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
            Font(Res.font.ibm_plex_sans_bold, FontWeight.Bold),
        )

val IbmPlexMono
    @Composable get() =
        FontFamily(
            Font(Res.font.ibm_plex_mono_regular, FontWeight.Normal),
            Font(Res.font.ibm_plex_mono_medium, FontWeight.Medium),
        )

val PttTypography
    @Composable get() =
        Typography(
            displayLarge =
                TextStyle(
                    fontFamily = IbmPlexSans,
                    fontWeight = FontWeight.Bold, // 700
                    fontSize = 34.sp,
                    letterSpacing = (-0.01).sp,
                ),
            headlineMedium =
                TextStyle(
                    fontFamily = IbmPlexSans,
                    fontWeight = FontWeight.SemiBold, // 600
                    fontSize = 24.sp,
                ),
            titleMedium =
                TextStyle(
                    fontFamily = IbmPlexSans,
                    fontWeight = FontWeight.SemiBold, // 600
                    fontSize = 16.sp,
                ),
            bodyLarge =
                TextStyle(
                    fontFamily = IbmPlexSans,
                    fontWeight = FontWeight.Normal, // 400
                    fontSize = 16.sp,
                ),
            bodyMedium =
                TextStyle(
                    fontFamily = IbmPlexSans,
                    fontWeight = FontWeight.Normal, // 400
                    fontSize = 14.sp,
                ),
            bodySmall =
                TextStyle(
                    fontFamily = IbmPlexSans,
                    fontWeight = FontWeight.Normal, // 400
                    fontSize = 13.sp,
                ),
            labelLarge =
                TextStyle(
                    fontFamily = IbmPlexSans,
                    fontWeight = FontWeight.SemiBold, // 600
                    fontSize = 14.sp,
                ),
            labelMedium =
                TextStyle(
                    fontFamily = IbmPlexMono,
                    fontWeight = FontWeight.Medium, // 500
                    fontSize = 14.sp,
                ),
            labelSmall =
                TextStyle(
                    fontFamily = IbmPlexMono,
                    fontWeight = FontWeight.Medium, // 500
                    fontSize = 11.sp,
                ),
        )
