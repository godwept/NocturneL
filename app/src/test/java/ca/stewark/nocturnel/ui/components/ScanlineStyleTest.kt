package ca.stewark.nocturnel.ui.components

import ca.stewark.nocturnel.ui.theme.ColorThemePreset
import ca.stewark.nocturnel.ui.theme.paletteFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanlineStyleTest {
    @Test fun effectsOnUsesVisiblePhosphorScanlinesOverBlackAndDarkensBrightContent() {
        val palette = paletteFor(ColorThemePreset.NEON_90S)
        val style = scanlineStyle(palette, enabled = true)

        assertNotNull(style)
        assertTrue(style!!.phosphorAlpha >= .10f)
        assertTrue(style.shadowAlpha >= .45f)
        assertEquals(palette.background, style.shadowColor)
        assertEquals(palette.scanlineTint, style.tintColor)
    }

    @Test fun effectsOffDrawsNoScanlineOverlay() {
        assertNull(scanlineStyle(paletteFor(ColorThemePreset.NEON_90S), enabled = false))
    }
}
