package ca.stewark.nocturnel.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptivePaletteTest {
    @Test fun `palette has at most sixteen source-derived colors`() {
        val pixels = IntArray(64) { i -> 0xFF000000.toInt() or (i * 3 shl 16) or (i * 2 shl 8) or i }
        val palette = AdaptivePaletteQuantizer().palette(PixelBuffer(8, 8, pixels))
        assertTrue(palette.size <= 16)
        assertEquals(palette, AdaptivePaletteQuantizer().palette(PixelBuffer(8, 8, pixels)))
    }

    @Test fun `transparent pixels are ignored and dominant red identity remains`() {
        val pixels = IntArray(32) { if (it < 24) 0xFFDD2010.toInt() else 0x0000FF00 }
        val palette = AdaptivePaletteQuantizer().palette(PixelBuffer(8, 4, pixels))
        assertTrue(palette.all { (it shr 16 and 0xFF) > (it shr 8 and 0xFF) })
    }
}
