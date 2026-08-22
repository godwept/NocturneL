package ca.stewark.nocturnel.ui.playback.visualizer

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test fun afterglowUsesTheComposeFrameClockWithoutIndependentTimers() {
        val source = File(
            "src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt",
        ).readText()

        assertTrue("withFrameNanos", "withFrameNanos" in source)
        assertTrue("rememberUpdatedState", "rememberUpdatedState" in source)
        listOf(
            "System.currentTimeMillis",
            "System.nanoTime",
            "elapsedRealtime",
            "rememberInfiniteTransition",
            "infiniteRepeatable",
            "delay(",
        ).forEach { forbidden -> assertFalse(forbidden, forbidden in source) }
    }
}
