package ca.stewark.nocturnel.ui.playback.visualizer

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class VisualizerSourceGuardTest {
    @Test fun removedVisualizerImplementationIsAbsent() {
        val sourceRoot = File("src/main/java/ca/stewark/nocturnel/ui/playback/visualizer")
        val source = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        listOf(
            "VisualizerDisplayMode.RING",
            "RingState",
            "RingEchoState",
            "RingGeometry",
            "RingSpike",
            "RingEcho",
            "ringGeometry",
            "ringMagnitudes",
            "visualizer-ring",
            "Terminal spectrum ring",
        ).forEach { removed -> assertFalse(removed, removed in source) }
    }
}
