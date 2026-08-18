package ca.stewark.nocturnel.ui.playback.visualizer

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class TunnelSourceGuardTest {
    @Test fun obsoleteOscilloscopeImplementationIsRemoved() {
        val sourceRoot = File("src/main/java/ca/stewark/nocturnel/ui/playback/visualizer")
        val source = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        assertFalse("VisualizerDisplayMode.SCOPE" in source)
        assertFalse("scopeGeometry" in source)
        assertFalse("visualizer-scope" in source)
        assertFalse("Oscilloscope" in source)
    }
}
