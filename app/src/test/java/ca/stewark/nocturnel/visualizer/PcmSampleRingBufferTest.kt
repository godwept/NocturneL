package ca.stewark.nocturnel.visualizer

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmSampleRingBufferTest {
    @Test fun returnsNewestWindowAndWraps() {
        val ring = PcmSampleRingBuffer(8)
        val output = FloatArray(4)
        assertFalse(ring.copyLatest(output))
        (1..10).forEach { ring.write(it.toFloat()) }
        assertTrue(ring.copyLatest(output))
        assertArrayEquals(floatArrayOf(7f, 8f, 9f, 10f), output, 0f)
    }

    @Test fun returnsWindowEndingAtAudiblePlaybackPosition() {
        val ring = PcmSampleRingBuffer(10)
        val output = FloatArray(4)
        (1..10).forEach { ring.write(it.toFloat()) }

        assertTrue(ring.copyLatest(output, samplesBehind = 2))

        assertArrayEquals(floatArrayOf(5f, 6f, 7f, 8f), output, 0f)
    }

    @Test fun resetRejectsOldSamples() {
        val ring = PcmSampleRingBuffer(8)
        repeat(4) { ring.write(it.toFloat()) }
        val generation = ring.generation
        ring.reset()
        assertTrue(ring.generation > generation)
        assertFalse(ring.copyLatest(FloatArray(4)))
    }
}
