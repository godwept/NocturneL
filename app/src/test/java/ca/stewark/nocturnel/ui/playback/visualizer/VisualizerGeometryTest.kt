package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerGeometryTest {
    private fun frame(
        waveform: List<Float> = List(128) { 0f },
        bands: List<Float> = List(32) { 0f },
        low: Float = 0f,
        mid: Float = 0f,
        high: Float = 0f,
        transient: Float = 0f,
        id: Long = 0,
    ) = AudioAnalysisFrame(waveform, bands, 0f, low, mid, high, transient, id, AnalysisStatus.ACTIVE)

    @Test fun radarUsesAllBandsAndPcmFrameForSweep() {
        val quiet = radarGeometry(frame(id = 7), 200f, 200f)
        val raised = radarGeometry(frame(bands = List(32) { if (it == 0) 1f else 0f }, transient = 1f, id = 8), 200f, 200f)
        assertEquals(32, quiet.spokeEndpoints.size)
        assertTrue(raised.spokeEndpoints.first().y < quiet.spokeEndpoints.first().y)
        assertEquals(14f, quiet.sweepDegrees, 0f)
        assertTrue(raised.echoRadius > raised.energyRadii.last())
        assertTrue(raised.spokeEndpoints.all { it.x in 0f..200f && it.y in 0f..200f })
    }

    @Test fun spectrumHasThirtyTwoBoundedColumns() {
        val bars = spectrumGeometry(frame(bands = List(32) { it / 31f }), 320f, 200f)
        assertEquals(32, bars.size)
        assertTrue(bars.zipWithNext().all { (a, b) -> a.left < b.left && a.segments <= b.segments })
        assertTrue(bars.all { it.peakY <= it.top && it.top <= it.bottom })
    }

    @Test fun scopeMapsWaveformFromBottomThroughCenterToTop() {
        val waveform = List(128) { index -> -1f + 2f * index / 127f }
        val points = scopeGeometry(frame(waveform = waveform), 200f, 200f)
        assertEquals(128, points.size)
        assertEquals(192f, points.first().y, .001f)
        assertEquals(8f, points.last().y, .001f)
        assertTrue(points.zipWithNext().all { (a, b) -> a.x < b.x })
    }
}
