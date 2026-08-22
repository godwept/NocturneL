package ca.stewark.nocturnel.ui.theme

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeWiringTest {
    @Test fun themeResolvesTypographyFromTheSelectedPreset() {
        val source = File("src/main/java/ca/stewark/nocturnel/ui/theme/Theme.kt").readText()

        assertTrue("fontPreset: FontPreset = FontPreset.DEFAULT" in source)
        assertTrue("typography = typographyFor(fontPreset)" in source)
    }

    @Test fun themeExposesSelectedSemanticPaletteAndEffects() {
        val source = File("src/main/java/ca/stewark/nocturnel/ui/theme/Theme.kt").readText()

        assertTrue("colorTheme: ColorThemePreset = ColorThemePreset.DEFAULT" in source)
        assertTrue("effectsEnabled: Boolean = true" in source)
        assertTrue("LocalTerminalPalette provides palette" in source)
        assertTrue("LocalTerminalEffectsEnabled provides effectsEnabled" in source)
    }

    @Test fun materialColorsMapFromSemanticRoles() {
        val palette = paletteFor(ColorThemePreset.NEON_90S)
        val colors = materialColorsFor(palette)

        assertEquals(palette.textPrimary, colors.primary)
        assertEquals(palette.textSecondary, colors.secondary)
        assertEquals(palette.accentPrimary, colors.tertiary)
        assertEquals(palette.background, colors.background)
        assertEquals(palette.panel, colors.surface)
        assertEquals(palette.textPrimary, colors.onSurface)
        assertEquals(palette.error, colors.error)
        assertEquals(palette.borderEmphasis, colors.outline)
        assertEquals(palette.border, colors.outlineVariant)
    }
}
