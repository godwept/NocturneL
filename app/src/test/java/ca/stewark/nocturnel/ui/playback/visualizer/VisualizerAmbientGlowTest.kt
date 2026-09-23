package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerAmbientGlowTest {
    private val active = AudioAnalysisFrame.Idle.copy(status = AnalysisStatus.ACTIVE, transient = .5f)

    @Test fun onlyActiveSquareAudioModesGlowWithEffects() {
        assertTrue(ambientGlowAlpha(VisualizerDisplayMode.RADAR, active, true) > 0f)
        assertTrue(ambientGlowAlpha(VisualizerDisplayMode.GRID, active, true) > 0f)
        assertEquals(0f, ambientGlowAlpha(VisualizerDisplayMode.BANDS, active, true))
        assertEquals(0f, ambientGlowAlpha(VisualizerDisplayMode.ART, active, true))
        assertEquals(0f, ambientGlowAlpha(VisualizerDisplayMode.RADAR, active, false))
        assertEquals(0f, ambientGlowAlpha(VisualizerDisplayMode.RADAR, AudioAnalysisFrame.Idle, true))
        assertEquals(0f, ambientGlowAlpha(VisualizerDisplayMode.RADAR, AudioAnalysisFrame.Unavailable, true))
    }

    @Test fun transientIsFiniteClampedAndMonotonic() {
        val mode = VisualizerDisplayMode.GRID
        assertEquals(0f, ambientGlowAlpha(mode, active.copy(transient = 0f), true))
        assertTrue(ambientGlowAlpha(mode, active.copy(transient = .2f), true) < ambientGlowAlpha(mode, active, true))
        assertEquals(ambientGlowAlpha(mode, active.copy(transient = 1f), true), ambientGlowAlpha(mode, active.copy(transient = 8f), true))
        for (invalid in listOf(-1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(0f, ambientGlowAlpha(mode, active.copy(transient = invalid), true))
        }
    }
}
