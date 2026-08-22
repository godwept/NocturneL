package ca.stewark.nocturnel.ui.playback.visualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerRenderingStyleTest {
    @Test fun radarBloomUsesApprovedWidthsAndOpacityTiers() {
        assertEquals(.16f, RADAR_GRID_BLOOM_ALPHA, 0f)
        assertEquals(4f, RADAR_GRID_BLOOM_WIDTH, 0f)
        assertEquals(.22f, RADAR_ENERGY_BLOOM_ALPHA, 0f)
        assertEquals(5f, RADAR_ENERGY_BLOOM_WIDTH, 0f)
        assertEquals(.20f, RADAR_SPOKE_BLOOM_ALPHA, 0f)
        assertEquals(4f, RADAR_SPOKE_BLOOM_WIDTH, 0f)
        assertEquals(.28f, RADAR_ECHO_BLOOM_MAX_ALPHA, 0f)
        assertEquals(6f, RADAR_ECHO_BLOOM_WIDTH, 0f)
        assertEquals(.45f, RADAR_TRAIL_BLOOM_ALPHA_SCALE, 0f)
        assertEquals(5f, RADAR_TRAIL_BLOOM_WIDTH, 0f)
        assertEquals(.36f, RADAR_SWEEP_BLOOM_ALPHA, 0f)
        assertEquals(7f, RADAR_SWEEP_BLOOM_WIDTH, 0f)
    }

    @Test fun movingBloomDominatesStaticBloomWithoutExceedingCores() {
        assertTrue(RADAR_GRID_BLOOM_ALPHA < RADAR_ENERGY_BLOOM_ALPHA)
        assertTrue(RADAR_SPOKE_BLOOM_ALPHA < RADAR_SWEEP_BLOOM_ALPHA)
        assertTrue(RADAR_ECHO_BLOOM_MAX_ALPHA < RADAR_SWEEP_BLOOM_ALPHA)
        assertTrue(RADAR_SWEEP_BLOOM_ALPHA < .90f)
        assertTrue(RADAR_AFTERGLOW_MAX_ALPHA * RADAR_TRAIL_BLOOM_ALPHA_SCALE < RADAR_AFTERGLOW_MAX_ALPHA)
    }
}
