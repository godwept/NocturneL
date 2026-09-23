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

    @Test fun radarBloomPrecedesCrispCoreWithoutPlatformBlur() {
        val source = File(
            "src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt",
        ).readText()
        val radarBranch = source
            .substringAfter("VisualizerDisplayMode.RADAR -> {")
            .substringBefore("VisualizerDisplayMode.BANDS ->")

        assertTrue("if (effectsEnabled)" in radarBranch.substringBefore("drawRadarCore("))
        assertTrue(radarBranch.indexOf("drawRadarBloom(") < radarBranch.indexOf("drawRadarCore("))
        assertFalse("RenderEffect" in source)
        assertFalse("BlurEffect" in source)
    }

    @Test fun bandGhostsUseLuminousPhosphorBehindLiveBars() {
        val source = File(
            "src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt",
        ).readText()
        val bandsBranch = source
            .substringAfter("VisualizerDisplayMode.BANDS -> {")
            .substringBefore("VisualizerDisplayMode.GRID ->")

        assertTrue(bandsBranch.indexOf("spectrumGhostGeometry(") < bandsBranch.indexOf("spectrumGeometry("))
        assertTrue("palette.visualizerPrimary.copy(alpha = afterglow.bands[ghost.bandIndex].alpha)" in bandsBranch)
        assertFalse("palette.textSecondary.copy(alpha = afterglow.bands" in bandsBranch)
        assertTrue(bandsBranch.lastIndexOf("palette.visualizerPeak") > bandsBranch.indexOf("spectrumGhostGeometry("))
    }

    @Test fun frequencyGridDrawsGhostBeforeLiveWithoutPlatformBlur() {
        val source = File(
            "src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt",
        ).readText()
        val gridBranch = source
            .substringAfter("VisualizerDisplayMode.GRID -> {")
            .substringBefore("VisualizerDisplayMode.ART ->")

        assertTrue("visualizer-grid" in source)
        assertTrue("frequencyGridGeometry(" in gridBranch)
        assertTrue(gridBranch.indexOf("ghostIntensity") < gridBranch.indexOf("liveIntensity"))
        assertTrue("palette.visualizerPeak" in gridBranch)
        assertFalse("RenderEffect" in source)
        assertFalse("BlurEffect" in source)
    }

    @Test fun fullScreenUsesExpandedSceneWhileNormalCallersDefaultToStandard() {
        val sceneSource = File(
            "src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt",
        ).readText()
        val fullScreenSource = File(
            "src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizer.kt",
        ).readText()

        assertTrue("expanded: Boolean = false" in sceneSource)
        assertTrue("expanded = true" in fullScreenSource)
    }

    @Test fun expandedGridUsesPortraitGeometryAndStandardGridKeepsSquareGeometry() {
        val source = File(
            "src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt",
        ).readText()
        val gridBranch = source
            .substringAfter("VisualizerDisplayMode.GRID -> {")
            .substringBefore("VisualizerDisplayMode.ART ->")

        assertTrue("if (expanded)" in gridBranch)
        assertTrue("frequencyGridPortraitGeometry(" in gridBranch)
        assertTrue("frequencyGridGeometry(" in gridBranch)
        assertTrue(
            gridBranch.indexOf("frequencyGridPortraitGeometry(") <
                gridBranch.indexOf("frequencyGridGeometry("),
        )
    }

    @Test fun fullScreenRadarPulseStateUsesTheExistingFrameClockAndEligibility() {
        val source = File(
            "src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt",
        ).readText()

        assertTrue("RadarFullScreenEffectState.Empty" in source)
        assertTrue("withFrameNanos" in source)
        assertTrue("updateRadarFullScreenEffects(" in source)
        assertTrue("expanded && mode == VisualizerDisplayMode.RADAR" in source)
        assertTrue("frame.status == AnalysisStatus.ACTIVE" in source)
    }

    @Test fun expandedRadarDrawsOuterEffectsBeforeTheExistingCore() {
        val source = File(
            "src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt",
        ).readText()
        val radarBranch = source
            .substringAfter("VisualizerDisplayMode.RADAR -> {")
            .substringBefore("VisualizerDisplayMode.BANDS ->")

        val pulse = radarBranch.indexOf("drawRadarFullScreenPulses(")
        val wake = radarBranch.indexOf("drawRadarExtendedWake(")
        val beam = radarBranch.indexOf("drawRadarExtendedBeam(")
        val bloom = radarBranch.indexOf("drawRadarBloom(")
        val core = radarBranch.indexOf("drawRadarCore(")

        assertTrue("expanded && effectsEnabled && frame.status == AnalysisStatus.ACTIVE" in radarBranch)
        assertTrue(pulse >= 0 && pulse < wake)
        assertTrue(wake < beam)
        assertTrue(beam < bloom)
        assertTrue(bloom < core)
    }

    @Test fun legacyMarginGlowImplementationIsAbsent() {
        val sourceRoot = File("src/main/java/ca/stewark/nocturnel/ui/playback/visualizer")
        val source = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .joinToString("\n") { it.readText() }

        assertFalse("ambientGlowAlpha" in source)
        assertFalse("full-screen-glow" in source)
    }

    @Test fun expandedRadarBeamUsesOutsideCoreFadeSegmentsOnly() {
        val source = File(
            "src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt",
        ).readText()
        val beamHelper = source
            .substringAfter("private fun DrawScope.drawRadarExtendedBeam(")
            .substringBefore("private fun DrawScope.drawRadarBloom(")

        assertTrue("radarExtendedBeamSegments(" in beamHelper)
        assertTrue("outerRadius = geometry.gridRadii.last()" in beamHelper)
        assertTrue("segment.start" in beamHelper)
        assertTrue("segment.end" in beamHelper)
        assertFalse(
            "drawLine(\n        palette.visualizerPeak.copy(alpha = RADAR_EXTENDED_BEAM_ALPHA)" in beamHelper,
        )
    }

    @Test fun standardGridKeepsUnscaledHotspotDistanceWhilePortraitUsesAspectScale() {
        val source = File(
            "src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt",
        ).readText()
        val square = source
            .substringAfter("internal fun frequencyGridGeometry(")
            .substringBefore("internal fun frequencyGridPortraitGeometry(")
        val portrait = source
            .substringAfter("internal fun frequencyGridPortraitGeometry(")
            .substringBefore("private fun frequencyGridGhostLevels(")

        assertTrue("hotspotYScale = 1f" in square)
        assertTrue("hotspotYScale = contentHeight / contentWidth" in portrait)
    }
}
