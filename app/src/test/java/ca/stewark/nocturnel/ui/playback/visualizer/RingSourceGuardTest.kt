package ca.stewark.nocturnel.ui.playback.visualizer

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class RingSourceGuardTest {
    @Test fun obsoleteTunnelImplementationIsRemoved() {
        val sourceRoot = File("src/main/java/ca/stewark/nocturnel/ui/playback/visualizer")
        val source = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        assertFalse("VisualizerDisplayMode.TUNNEL" in source)
        assertFalse("tunnelGeometry" in source)
        assertFalse("TunnelGeometry" in source)
        assertFalse("TunnelHistory" in source)
        assertFalse("visualizer-tunnel" in source)
        assertFalse("Kaleidoscope tunnel" in source)
    }
}
