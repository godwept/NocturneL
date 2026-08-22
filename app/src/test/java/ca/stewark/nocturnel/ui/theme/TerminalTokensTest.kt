package ca.stewark.nocturnel.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalTokensTest {
    @Test fun greenPalettePreservesTheExistingTerminalColors() {
        val palette = paletteFor(ColorThemePreset.GREEN_TERMINAL)
        assertArgb(0xFF000000, palette.background)
        assertArgb(0xFF050805, palette.panel)
        assertArgb(0xFF00FF41, palette.textPrimary)
        assertArgb(0xFF00B32D, palette.textSecondary)
        assertArgb(0xFF008020, palette.textMuted)
        assertArgb(0xFF00B32D, palette.border)
        assertArgb(0xFF00FF41, palette.borderEmphasis)
        assertArgb(0xFFFFB000, palette.selection)
        assertArgb(0xFFFFB000, palette.warning)
        assertArgb(0xFFFF3030, palette.error)
        assertArgb(0xFF00FF41, palette.visualizerPrimary)
        assertArgb(0xFF008020, palette.visualizerSecondary)
        assertArgb(0xFF39FF7C, palette.visualizerPeak)
        assertEquals(.48f, palette.scanlineShadowAlpha)
        assertEquals(.12f, palette.scanlineTintAlpha)
        assertEquals(0f, palette.glowStrength)
        assertEquals(
            listOf(0xFF00E676, 0xFF00BFA5, 0xFF76FF03, 0xFF64DD17).map(Long::toInt),
            palette.artworkPlaceholderColors.map { it.toArgb() },
        )
    }

    @Test fun everyPresetHasACompleteReadablePalette() {
        ColorThemePreset.entries.forEach { preset ->
            val palette = paletteFor(preset)
            listOf(palette.textPrimary, palette.textSecondary, palette.warning, palette.error).forEach { color ->
                assertTrue("$preset color must contrast with background", contrast(color.toArgb(), palette.background.toArgb()) >= 4.5)
                assertTrue("$preset color must contrast with panel", contrast(color.toArgb(), palette.panel.toArgb()) >= 4.5)
            }
            listOf(palette.textMuted, palette.selection).forEach { color ->
                assertTrue("$preset secondary role must contrast with background", contrast(color.toArgb(), palette.background.toArgb()) >= 3.0)
                assertTrue("$preset secondary role must contrast with panel", contrast(color.toArgb(), palette.panel.toArgb()) >= 3.0)
            }
            assertEquals(4, palette.artworkPlaceholderColors.size)
            assertEquals(preset == ColorThemePreset.NEON_90S, palette.glowStrength > 0f)
        }
    }

    private fun assertArgb(expected: Long, actual: androidx.compose.ui.graphics.Color) =
        assertEquals(expected.toInt(), actual.toArgb())

    private fun contrast(first: Int, second: Int): Double {
        val firstLuminance = luminance(first)
        val secondLuminance = luminance(second)
        return (maxOf(firstLuminance, secondLuminance) + .05) /
            (minOf(firstLuminance, secondLuminance) + .05)
    }

    private fun luminance(argb: Int): Double {
        fun channel(shift: Int): Double {
            val value = ((argb ushr shift) and 0xFF) / 255.0
            return if (value <= .04045) value / 12.92 else Math.pow((value + .055) / 1.055, 2.4)
        }
        return .2126 * channel(16) + .7152 * channel(8) + .0722 * channel(0)
    }
}
