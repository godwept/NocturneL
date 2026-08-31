package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerAfterglowTest {
    @Test fun afterglowIsPronouncedButSubordinateToLiveElements() {
        assertEquals(500_000_000L, AFTERGLOW_DURATION_NANOS)
        assertEquals(.70f, RADAR_AFTERGLOW_MAX_ALPHA, 0f)
        assertEquals(.72f, BAND_AFTERGLOW_MAX_ALPHA, 0f)
        assertTrue(RADAR_AFTERGLOW_MAX_ALPHA < .90f)
        assertTrue(BAND_AFTERGLOW_MAX_ALPHA < 1f)
    }

    @Test fun firstRadarFrameHasNoInventedTrail() {
        val state = updateRadarAfterglow(RadarAfterglowState(), 1, 358f, 0)

        assertEquals(1L, state.currentFrameId)
        assertEquals(358f, state.currentAngleDegrees, 0f)
        assertTrue(state.samples.isEmpty())
    }

    @Test fun newRadarFramesRetainAtMostSixteenPriorAngles() {
        var state = RadarAfterglowState()
        repeat(20) { index ->
            state = updateRadarAfterglow(state, index.toLong(), index * 2f, 1_000_000)
        }

        assertEquals(16, state.samples.size)
        assertEquals((6..36 step 2).map(Int::toFloat), state.samples.map { it.angleDegrees })
    }

    @Test fun repeatedFrameIdAgesWithoutDuplicatingSweep() {
        var state = updateRadarAfterglow(RadarAfterglowState(), 1, 0f, 0)
        state = updateRadarAfterglow(state, 2, 2f, 10_000_000)
        val initialAlpha = state.samples.single().alpha

        state = updateRadarAfterglow(state, 2, 2f, 50_000_000)

        assertEquals(1, state.samples.size)
        assertTrue(state.samples.single().alpha < initialAlpha)
    }

    @Test fun radarTrailFadesMonotonicallyAndExpiresAt500Milliseconds() {
        var state = updateRadarAfterglow(RadarAfterglowState(), 1, 0f, 0)
        state = updateRadarAfterglow(state, 2, 2f, 0)
        val freshAlpha = state.samples.single().alpha
        state = updateRadarAfterglow(state, 2, 2f, 250_000_000)
        val halfAlpha = state.samples.single().alpha
        state = updateRadarAfterglow(state, 2, 2f, 249_000_000)
        val nearExpiryAlpha = state.samples.single().alpha

        assertTrue(freshAlpha in 0f..RADAR_AFTERGLOW_MAX_ALPHA)
        assertTrue(halfAlpha in 0f..<freshAlpha)
        assertTrue(nearExpiryAlpha in 0f..<halfAlpha)
        state = updateRadarAfterglow(state, 2, 2f, 1_000_000)
        assertTrue(state.samples.isEmpty())
    }

    @Test fun radarAnglesNormalizeAcross360WithoutConnectedWrapGeometry() {
        var state = updateRadarAfterglow(RadarAfterglowState(), 1, 358f, 0)
        state = updateRadarAfterglow(state, 2, 360f, 1)
        state = updateRadarAfterglow(state, 3, -2f, 1)

        assertEquals(listOf(358f, 0f), state.samples.map { it.angleDegrees })
        assertEquals(358f, state.currentAngleDegrees, 0f)
        assertTrue(state.samples.all { it.angleDegrees in 0f..<360f })
    }

    @Test fun decreasingRadarFrameIdStartsFresh() {
        var state = updateRadarAfterglow(RadarAfterglowState(), 2, 4f, 0)
        state = updateRadarAfterglow(state, 3, 6f, 1)
        state = updateRadarAfterglow(state, 1, 2f, 1)

        assertEquals(1L, state.currentFrameId)
        assertTrue(state.samples.isEmpty())
    }

    @Test fun largeRadarFrameGapDoesNotRevivePreviousArm() {
        var state = updateRadarAfterglow(RadarAfterglowState(), 1, 0f, 0)
        state = updateRadarAfterglow(state, 2, 2f, AFTERGLOW_DURATION_NANOS)

        assertEquals(2L, state.currentFrameId)
        assertTrue(state.samples.isEmpty())
    }

    @Test fun risingBandReplacesEnvelopeWithoutVisibleGhost() {
        var state = updateBandAfterglow(emptyList(), listOf(.2f), 0)
        state = updateBandAfterglow(state, listOf(.8f), 16_000_000)

        assertEquals(.8f, state.single().retainedLevel, 0f)
        assertEquals(0f, state.single().alpha, 0f)
    }

    @Test fun fallingBandRetainsThenLowersAndFadesFormerHeight() {
        var state = updateBandAfterglow(emptyList(), listOf(1f), 0)
        state = updateBandAfterglow(state, listOf(.1f), 1)
        val fresh = state.single()
        state = updateBandAfterglow(state, listOf(.1f), 250_000_000)
        val halfway = state.single()

        assertTrue(fresh.retainedLevel > halfway.retainedLevel)
        assertTrue(halfway.retainedLevel > .1f)
        assertTrue(fresh.alpha in 0f..BAND_AFTERGLOW_MAX_ALPHA)
        assertTrue(halfway.alpha in 0f..<fresh.alpha)
    }

    @Test fun bandEnvelopeNeverFallsBelowLiveValue() {
        var state = updateBandAfterglow(emptyList(), listOf(.7f), 0)
        state = updateBandAfterglow(state, listOf(.6f), 249_000_000)

        assertTrue(state.single().retainedLevel >= .6f)
    }

    @Test fun bandGhostRemainsBeforeAndExpiresAt500Milliseconds() {
        var state = updateBandAfterglow(emptyList(), listOf(1f), 0)
        state = updateBandAfterglow(state, listOf(0f), 1)
        state = updateBandAfterglow(state, listOf(0f), AFTERGLOW_DURATION_NANOS - 1_000_001)

        assertTrue(state.single().retainedLevel > 0f)
        assertTrue(state.single().alpha > 0f)

        state = updateBandAfterglow(state, listOf(0f), 1_000_000)

        assertEquals(0f, state.single().retainedLevel, 0f)
        assertEquals(0f, state.single().alpha, 0f)
    }

    @Test fun bandCountChangeRebuildsWithoutGhosts() {
        var state = updateBandAfterglow(emptyList(), listOf(1f), 0)
        state = updateBandAfterglow(state, listOf(.1f, .2f), 16_000_000)

        assertEquals(listOf(.1f, .2f), state.map { it.retainedLevel })
        assertTrue(state.all { it.alpha == 0f })
    }

    @Test fun malformedBandValuesAreFiniteAndClamped() {
        val state = updateBandAfterglow(
            emptyList(),
            listOf(Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, -1f, 2f),
            0,
        )

        assertEquals(listOf(0f, 0f, 0f, 0f, 1f), state.map { it.retainedLevel })
        assertTrue(state.all { it.retainedLevel.isFinite() })
    }

    @Test fun unifiedStateClearsForIneligibleLifecycleAndRestartsFresh() {
        val size = VisualizerSizeKey(200, 200)
        var state = updateVisualizerAfterglow(
            VisualizerAfterglowState.Empty,
            VisualizerDisplayMode.RADAR,
            frame(1),
            true,
            size,
            0,
        )
        state = updateVisualizerAfterglow(state, VisualizerDisplayMode.RADAR, frame(2), true, size, 1)
        assertEquals(1, state.radar.samples.size)

        listOf(
            updateVisualizerAfterglow(state, VisualizerDisplayMode.ART, frame(3), true, size, 1),
            updateVisualizerAfterglow(state, VisualizerDisplayMode.RADAR, frame(3), false, size, 1),
            updateVisualizerAfterglow(state, VisualizerDisplayMode.RADAR, frame(3, AnalysisStatus.IDLE), true, size, 1),
            updateVisualizerAfterglow(state, VisualizerDisplayMode.RADAR, frame(3, AnalysisStatus.UNAVAILABLE), true, size, 1),
        ).forEach { cleared -> assertEquals(VisualizerAfterglowState.Empty, cleared) }

        val restarted = updateVisualizerAfterglow(
            VisualizerAfterglowState.Empty,
            VisualizerDisplayMode.RADAR,
            frame(4),
            true,
            size,
            1,
        )
        assertTrue(restarted.radar.samples.isEmpty())
    }

    @Test fun unifiedStateClearsForSizeOrDecreasingFrameAndIsolatesModes() {
        val size = VisualizerSizeKey(200, 200)
        var radar = updateVisualizerAfterglow(
            VisualizerAfterglowState.Empty,
            VisualizerDisplayMode.RADAR,
            frame(2),
            true,
            size,
            0,
        )
        radar = updateVisualizerAfterglow(radar, VisualizerDisplayMode.RADAR, frame(3), true, size, 1)
        val resized = updateVisualizerAfterglow(radar, VisualizerDisplayMode.RADAR, frame(4), true, VisualizerSizeKey(201, 200), 1)
        val rewound = updateVisualizerAfterglow(radar, VisualizerDisplayMode.RADAR, frame(1), true, size, 1)

        assertTrue(resized.radar.samples.isEmpty())
        assertTrue(rewound.radar.samples.isEmpty())
        assertTrue(radar.bands.isEmpty())

        val bands = updateVisualizerAfterglow(
            radar,
            VisualizerDisplayMode.BANDS,
            frame(4),
            true,
            size,
            1,
        )
        assertTrue(bands.radar.samples.isEmpty())
        assertEquals(2, bands.bands.size)
    }

    @Test fun gridReusesBandEnvelopeAndLifecycleClearing() {
        val size = VisualizerSizeKey(200, 200)
        var grid = updateVisualizerAfterglow(
            VisualizerAfterglowState.Empty,
            VisualizerDisplayMode.GRID,
            frame(2),
            true,
            size,
            0,
        )
        assertEquals(VisualizerDisplayMode.GRID, grid.activeMode)
        assertEquals(2, grid.bands.size)
        assertTrue(grid.radar.samples.isEmpty())

        grid = updateVisualizerAfterglow(grid, VisualizerDisplayMode.GRID, frame(3), true, size, 1)
        assertEquals(2, grid.bands.size)
        assertEquals(
            VisualizerAfterglowState.Empty,
            updateVisualizerAfterglow(grid, VisualizerDisplayMode.GRID, frame(4), false, size, 1),
        )
        assertTrue(updateVisualizerAfterglow(grid, VisualizerDisplayMode.GRID, frame(1), true, size, 1).bands.all { it.alpha == 0f })
    }

    private fun frame(id: Long, status: AnalysisStatus = AnalysisStatus.ACTIVE) = AudioAnalysisFrame(
        waveform = emptyList(),
        bands = listOf(.8f, .2f),
        energy = 0f,
        lowEnergy = 0f,
        midEnergy = 0f,
        highEnergy = 0f,
        transient = 0f,
        frameId = id,
        status = status,
    )
}
