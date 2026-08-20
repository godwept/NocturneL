package ca.stewark.nocturnel.visualizer

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualizerSyncOffsetTest {
    @Test fun clampsAdjustsAndFormatsOffsets() {
        assertEquals(0, VisualizerSyncOffset.DEFAULT_MS)
        assertEquals(-2_000, VisualizerSyncOffset.MIN_MS)
        assertEquals(2_000, VisualizerSyncOffset.MAX_MS)
        assertEquals(25, VisualizerSyncOffset.STEP_MS)
        assertEquals(0, VisualizerSyncOffset.clamp(0))
        assertEquals(-2_000, VisualizerSyncOffset.clamp(-9_999))
        assertEquals(2_000, VisualizerSyncOffset.clamp(9_999))
        assertEquals(25, VisualizerSyncOffset.increase(0))
        assertEquals(-25, VisualizerSyncOffset.decrease(0))
        assertEquals(2_000, VisualizerSyncOffset.increase(2_000))
        assertEquals(-2_000, VisualizerSyncOffset.decrease(-2_000))
        assertEquals("+150 ms", VisualizerSyncOffset.label(150))
        assertEquals("-25 ms", VisualizerSyncOffset.label(-25))
        assertEquals("0 ms", VisualizerSyncOffset.label(0))
    }
}
