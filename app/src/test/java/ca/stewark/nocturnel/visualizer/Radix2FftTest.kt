package ca.stewark.nocturnel.visualizer

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class Radix2FftTest {
    @Test fun rejectsNonPowerOfTwo() {
        assertThrows(IllegalArgumentException::class.java) { Radix2Fft(7) }
    }

    @Test fun locatesToneAndClearsBetweenTransforms() {
        val fft = Radix2Fft(2_048)
        val output = FloatArray(1_025)
        val input = FloatArray(2_048) { sin(2 * PI * 1_000 * it / 48_000).toFloat() }
        fft.magnitudes(input, output)
        val peak = output.indices.maxBy { output[it] }
        assertTrue(kotlin.math.abs(peak - (1_000 * 2_048 / 48_000)) <= 1)
        fft.magnitudes(FloatArray(2_048), output)
        assertEquals(0f, output.maxOrNull() ?: -1f, .000001f)
    }
}
