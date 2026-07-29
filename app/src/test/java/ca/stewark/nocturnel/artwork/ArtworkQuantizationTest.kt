package ca.stewark.nocturnel.artwork

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkQuantizationTest {
    @Test fun `quantization is deterministic sharp and bounded`() {
        val pixels = IntArray(256) { i -> 0xFF000000.toInt() or (i shl 16) or ((255 - i) shl 8) or i }
        val input = PixelBuffer(16, 16, pixels)
        val first = AdaptivePaletteQuantizer().quantize(input)
        val second = AdaptivePaletteQuantizer().quantize(input)
        assertEquals(16, first.width)
        assertEquals(16, first.height)
        assertArrayEquals(first.pixels(), second.pixels())
        assertTrue(first.pixels().map { it and 0x00FFFFFF }.distinct().size <= 16)
        assertTrue(first.pixels().all { it ushr 24 == 0xFF })
    }
}
