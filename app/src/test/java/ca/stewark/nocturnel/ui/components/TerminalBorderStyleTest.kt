package ca.stewark.nocturnel.ui.components

import ca.stewark.nocturnel.ui.theme.ColorThemePreset
import ca.stewark.nocturnel.ui.theme.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalBorderStyleTest {
    @Test fun glowRequiresEffectsAndANeonPalette() {
        val green = paletteFor(ColorThemePreset.GREEN_TERMINAL)
        val neon = paletteFor(ColorThemePreset.NEON_90S)

        assertEquals(0f, terminalBorderStyle(green, effectsEnabled = true, emphasized = true).glowAlpha)
        assertEquals(0f, terminalBorderStyle(neon, effectsEnabled = false, emphasized = true).glowAlpha)
        assertEquals(.12f, terminalBorderStyle(neon, effectsEnabled = true, emphasized = false).glowAlpha)
        assertEquals(.24f, terminalBorderStyle(neon, effectsEnabled = true, emphasized = true).glowAlpha)
    }
}
