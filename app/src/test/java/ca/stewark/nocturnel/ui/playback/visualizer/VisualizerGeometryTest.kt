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

    @Test fun frequencyGridIsDenseSquareCenteredAndDeterministic() {
        val first = frequencyGridGeometry(List(32) { 0f }, emptyList(), 320f, 200f)
        val second = frequencyGridGeometry(List(32) { 0f }, emptyList(), 320f, 200f)

        assertEquals(900, first.size)
        assertEquals(first, second)
        assertTrue(first.all { cell ->
            cell.size > 0f && listOf(cell.left, cell.top, cell.size).all(Float::isFinite) &&
                cell.left >= 0f && cell.top >= 0f &&
                cell.left + cell.size <= 320f && cell.top + cell.size <= 200f
        })
        assertTrue(first.all { it.size == first.first().size })
        assertTrue(first.zipWithNext().take(29).all { (a, b) -> b.left > a.left && b.top == a.top })
        assertTrue(first[30].top > first[0].top)
        assertEquals(160f, (first.first().left + first[29].left + first[29].size) / 2f, .001f)
        assertEquals(100f, (first.first().top + first.last().top + first.last().size) / 2f, .001f)
    }

    @Test fun frequencyAnchorsAreFixedUniqueInsetAndDistributed() {
        val peaks = List(32) { activeBand ->
            val levels = List(32) { if (it == activeBand) 1f else 0f }
            frequencyGridGeometry(levels, emptyList(), 300f, 300f).maxBy { it.liveIntensity }
        }

        assertEquals(32, peaks.map { it.left to it.top }.toSet().size)
        assertTrue(peaks.all { it.left > 0f && it.top > 0f && it.left + it.size < 300f && it.top + it.size < 300f })
        assertTrue(peaks.any { it.left < 150f && it.top < 150f })
        assertTrue(peaks.any { it.left >= 150f && it.top < 150f })
        assertTrue(peaks.any { it.left < 150f && it.top >= 150f })
        assertTrue(peaks.any { it.left >= 150f && it.top >= 150f })
    }

    @Test fun frequencyHotspotsFallOffBlendClampAndGhost() {
        val first = frequencyGridGeometry(List(32) { if (it == 0) 1f else 0f }, emptyList(), 300f, 300f)
        val second = frequencyGridGeometry(List(32) { if (it == 1) 1f else 0f }, emptyList(), 300f, 300f)
        val both = frequencyGridGeometry(List(32) { if (it <= 1) 1f else 0f }, emptyList(), 300f, 300f)
        val peak = first.maxBy { it.liveIntensity }

        assertTrue(peak.liveIntensity > first.minOf { it.liveIntensity })
        assertTrue(both.indices.any { both[it].liveIntensity > maxOf(first[it].liveIntensity, second[it].liveIntensity) })
        assertTrue(both.all { it.liveIntensity in 0f..1f })

        val malformed = frequencyGridGeometry(
            listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 2f),
            listOf(BandAfterglow(1f, 1f, 1L)),
            120f,
            80f,
        )
        assertTrue(malformed.all { it.liveIntensity.isFinite() && it.liveIntensity in 0f..1f && it.ghostIntensity == 0f })

        val ghost = frequencyGridGeometry(
            listOf(0f),
            listOf(BandAfterglow(1f, 1f, 1L)),
            120f,
            120f,
        )
        assertTrue(ghost.any { it.ghostIntensity > 0f })
        assertTrue(frequencyGridGeometry(emptyList(), emptyList(), 0f, 0f).isEmpty())
        assertTrue(frequencyGridGeometry(listOf(1f), emptyList(), -1f, 20f).isEmpty())
    }
}
