package ca.stewark.nocturnel.ui.playback.visualizer

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualizerDisplayModeTest {
    @Test fun cyclesInApprovedOrder() {
        assertEquals(VisualizerDisplayMode.RADAR, VisualizerDisplayMode.ART.next())
        assertEquals(VisualizerDisplayMode.BANDS, VisualizerDisplayMode.RADAR.next())
        assertEquals(VisualizerDisplayMode.ART, VisualizerDisplayMode.BANDS.next())
    }

    @Test fun exposesApprovedLabels() {
        assertEquals(
            listOf("ART 1/3", "RADAR 2/3", "BANDS 3/3"),
            VisualizerDisplayMode.entries.map { it.label },
        )
        assertEquals(
            listOf("Album art", "Circular radar", "Spectrum bars"),
            VisualizerDisplayMode.entries.map { it.accessibilityName },
        )
    }
}
