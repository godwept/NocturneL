package ca.stewark.nocturnel.ui.playback.visualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerSessionStateTest {
    @Test fun opensFromArtAtRadarAndRetainsTheLastAudioMode() {
        val initial = VisualizerSessionState()
        assertEquals(VisualizerDisplayMode.ART, initial.mode)
        assertFalse(initial.expanded)

        val radar = initial.open()
        assertEquals(VisualizerDisplayMode.RADAR, radar.mode)
        assertTrue(radar.expanded)
        val grid = radar.swipeLeft().swipeLeft()
        assertEquals(VisualizerDisplayMode.GRID, grid.mode)
        assertEquals(VisualizerDisplayMode.GRID, grid.close().mode)
        assertEquals(grid, grid.close().open())

        for (mode in listOf(VisualizerDisplayMode.RADAR, VisualizerDisplayMode.BANDS, VisualizerDisplayMode.GRID)) {
            assertEquals(mode, VisualizerSessionState(mode).open().mode)
        }
    }

    @Test fun horizontalSwipesWrapOnlyWhileExpanded() {
        val radar = VisualizerSessionState().open()
        assertEquals(VisualizerDisplayMode.BANDS, radar.swipeLeft().mode)
        assertEquals(VisualizerDisplayMode.GRID, radar.swipeRight().mode)
        assertEquals(VisualizerDisplayMode.RADAR, radar.swipeLeft().swipeLeft().swipeLeft().mode)
        assertEquals(VisualizerDisplayMode.RADAR, radar.swipeRight().swipeRight().swipeRight().mode)
        assertEquals(radar.close(), radar.close().swipeLeft())
        assertEquals(radar.close(), radar.close().swipeRight())
    }

    @Test fun analysisContinuesAcrossThePresentationSwitch() {
        val audio = VisualizerSessionState(VisualizerDisplayMode.BANDS)
        assertTrue(audio.analysisNeeded(nowVisible = true))
        assertTrue(audio.open().analysisNeeded(nowVisible = false))
        assertTrue(audio.open().close().analysisNeeded(nowVisible = true))
        assertFalse(audio.analysisNeeded(nowVisible = false))
        assertFalse(VisualizerSessionState().analysisNeeded(nowVisible = true))
    }
}
