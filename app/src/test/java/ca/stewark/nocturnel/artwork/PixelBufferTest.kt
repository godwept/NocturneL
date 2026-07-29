package ca.stewark.nocturnel.artwork

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PixelBufferTest {
    @Test fun `pixel count and dimensions are validated`() {
        assertThrows(IllegalArgumentException::class.java) { PixelBuffer(0, 1, intArrayOf()) }
        assertThrows(IllegalArgumentException::class.java) { PixelBuffer(2, 2, intArrayOf(1)) }
    }

    @Test fun `pixels including alpha are retained and copied`() {
        val source = intArrayOf(0x80112233.toInt())
        val buffer = PixelBuffer(1, 1, source)
        source[0] = 0
        assertArrayEquals(intArrayOf(0x80112233.toInt()), buffer.pixels())
    }
}
