package com.pttlan.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.pttlan.core.designsystem.theme.AccentTxDark
import com.pttlan.core.designsystem.theme.BgDark
import com.pttlan.core.designsystem.theme.IconOnAccentTxDark
import com.pttlan.core.designsystem.theme.IconOnPrimaryDark
import com.pttlan.core.designsystem.theme.PrimaryDark
import com.pttlan.core.designsystem.theme.StatusOfflineDark
import com.pttlan.core.designsystem.theme.StatusOnlineDark
import com.pttlan.core.designsystem.theme.Surface2Dark
import com.pttlan.core.designsystem.theme.TextPrimaryDark
import com.pttlan.core.designsystem.theme.TextSecondaryDark
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The watch runs no Kotlin, so its palette is copied into Swift (33.1), each color named after its constant here.
 * A palette change in `Color.kt` fails this test until the watch follows.
 */
class WatchPaletteTest {
    private val watchTheme = File("../iosApp/watchApp/WatchTheme.swift")

    private val palette: Map<String, Color> =
        mapOf(
            "BgDark" to BgDark,
            "Surface2Dark" to Surface2Dark,
            "TextPrimaryDark" to TextPrimaryDark,
            "TextSecondaryDark" to TextSecondaryDark,
            "PrimaryDark" to PrimaryDark,
            "AccentTxDark" to AccentTxDark,
            "StatusOnlineDark" to StatusOnlineDark,
            "StatusOfflineDark" to StatusOfflineDark,
            "IconOnAccentTxDark" to IconOnAccentTxDark,
            "IconOnPrimaryDark" to IconOnPrimaryDark,
        )

    @Test
    fun everyWatchColorMatchesItsDesignSystemConstant() {
        val colors =
            Regex("""Color\(hex: 0x([0-9A-Fa-f]{6})\) // (\w+)""")
                .findAll(watchTheme.readText())
                .map { it.groupValues[2] to it.groupValues[1].toInt(HEX) }
                .toList()

        assertTrue(colors.isNotEmpty(), "found no colors in ${watchTheme.absolutePath}")
        colors.forEach { (name, rgb) ->
            val expected = palette[name] ?: error("$name is not a palette color known to this test")
            assertEquals(expected.toArgb() and RGB_MASK, rgb, "$name differs between Color.kt and the watch")
        }
    }

    private companion object {
        const val HEX = 16
        const val RGB_MASK = 0xFFFFFF
    }
}
