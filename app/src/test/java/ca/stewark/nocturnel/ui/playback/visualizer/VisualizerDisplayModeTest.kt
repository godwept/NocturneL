package ca.stewark.nocturnel.ui.playback.visualizer

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualizerDisplayModeTest {
    @Test fun cyclesInApprovedOrder() {
        assertEquals(VisualizerDisplayMode.RADAR, VisualizerDisplayMode.ART.next())
        assertEquals(VisualizerDisplayMode.BANDS, VisualizerDisplayMode.RADAR.next())
        assertEquals(VisualizerDisplayMode.GRID, VisualizerDisplayMode.BANDS.next())
        assertEquals(VisualizerDisplayMode.ART, VisualizerDisplayMode.GRID.next())
    }

    @Test fun exposesApprovedLabels() {
        assertEquals(
            listOf("ART 1/4", "RADAR 2/4", "BANDS 3/4", "GRID 4/4"),
            VisualizerDisplayMode.entries.map { it.label },
        )
        assertEquals(
            listOf("Album art", "Circular radar", "Spectrum bars", "Frequency grid"),
            VisualizerDisplayMode.entries.map { it.accessibilityName },
        )
    }
}
