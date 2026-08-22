package ca.stewark.nocturnel.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorThemePresetTest {
    @Test fun stableValuesLabelsAndCycleOrderAreFixed() {
        assertEquals(
            listOf("green_terminal", "amber_terminal", "blue_terminal", "80s_synthwave", "90s_neon"),
            ColorThemePreset.entries.map { it.persistedValue },
        )
        assertEquals(
            listOf("GREEN TERMINAL", "AMBER TERMINAL", "BLUE TERMINAL", "'80S SYNTHWAVE", "'90S NEON"),
            ColorThemePreset.entries.map { it.label },
        )
        assertEquals(ColorThemePreset.AMBER_TERMINAL, ColorThemePreset.GREEN_TERMINAL.next())
        assertEquals(ColorThemePreset.BLUE_TERMINAL, ColorThemePreset.AMBER_TERMINAL.next())
        assertEquals(ColorThemePreset.SYNTHWAVE_80S, ColorThemePreset.BLUE_TERMINAL.next())
        assertEquals(ColorThemePreset.NEON_90S, ColorThemePreset.SYNTHWAVE_80S.next())
        assertEquals(ColorThemePreset.GREEN_TERMINAL, ColorThemePreset.NEON_90S.next())
    }

    @Test fun persistedValuesRestoreOrFallBackToGreen() {
        assertEquals(ColorThemePreset.GREEN_TERMINAL, ColorThemePreset.fromPersisted(null))
        assertEquals(ColorThemePreset.GREEN_TERMINAL, ColorThemePreset.fromPersisted("unknown"))
        assertEquals(ColorThemePreset.NEON_90S, ColorThemePreset.fromPersisted("90s_neon"))
    }
}
