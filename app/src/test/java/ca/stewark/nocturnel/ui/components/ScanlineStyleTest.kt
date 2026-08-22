package ca.stewark.nocturnel.ui.components

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanlineStyleTest {
    @Test fun effectsOnUsesVisiblePhosphorScanlinesOverBlackAndDarkensBrightContent() {
        val style = scanlineStyle(enabled = true)

        assertNotNull(style)
        assertTrue(style!!.phosphorAlpha >= .10f)
        assertTrue(style.shadowAlpha >= .45f)
    }

    @Test fun effectsOffDrawsNoScanlineOverlay() {
        assertNull(scanlineStyle(enabled = false))
    }
}
