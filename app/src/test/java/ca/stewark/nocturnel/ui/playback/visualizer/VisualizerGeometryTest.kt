package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

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

    @Test fun radarSweepEndpointNormalizesEquivalentAnglesOnRequestedRadius() {
        val center = VisualizerPoint(100f, 80f)
        val angles = listOf(0f, 90f, 180f, 270f, 358f, 360f, -2f)
        val points = angles.associateWith { radarSweepEndpoint(center, 40f, it) }

        points.values.forEach { point ->
            assertEquals(40f, hypot(point.x - center.x, point.y - center.y), .001f)
        }
        assertEquals(points.getValue(0f).x, points.getValue(360f).x, .001f)
        assertEquals(points.getValue(0f).y, points.getValue(360f).y, .001f)
        assertEquals(points.getValue(358f).x, points.getValue(-2f).x, .001f)
        assertEquals(points.getValue(358f).y, points.getValue(-2f).y, .001f)
    }

    @Test fun spectrumGhostsOnlyOccupySegmentsAboveLiveBars() {
        val equal = spectrumGhostGeometry(listOf(.5f), listOf(.5f), 120f, 120f)
        val raised = spectrumGhostGeometry(listOf(.25f), listOf(.75f), 120f, 120f).single()
        val live = spectrumGeometry(listOf(.25f), 120f, 120f).single()
        val retained = spectrumGeometry(listOf(.75f), 120f, 120f).single()

        assertTrue(equal.isEmpty())
        assertEquals(live.segments, raised.firstSegment)
        assertEquals(retained.segments - live.segments, raised.segments)
        assertTrue(raised.firstSegment >= live.segments)
    }

    @Test fun zeroLiveBandCanShowBoundedGhostUsingLiveSegmentPitch() {
        val ghost = spectrumGhostGeometry(listOf(0f), listOf(1f), 120f, 120f).single()

        assertEquals(0, ghost.firstSegment)
        assertTrue(ghost.segments > 0)
        assertEquals(8f, ghost.left, 0f)
        assertEquals(112f, ghost.right, 0f)
        assertEquals(112f, ghost.bottom, 0f)
    }

    @Test fun spectrumGeometryHandlesEmptyTinyAndNonSquareCanvases() {
        assertTrue(spectrumGhostGeometry(emptyList(), emptyList(), 0f, 0f).isEmpty())
        listOf(
            spectrumGeometry(listOf(1f), 1f, 1f),
            spectrumGeometry(listOf(1f, .5f), 40f, 200f),
        ).flatten().forEach { bar ->
            assertTrue(listOf(bar.left, bar.right, bar.top, bar.bottom, bar.peakY).all { it.isFinite() })
            assertTrue(bar.left >= 0f && bar.right <= 40f)
            assertTrue(bar.top <= bar.bottom)
        }
    }
}
