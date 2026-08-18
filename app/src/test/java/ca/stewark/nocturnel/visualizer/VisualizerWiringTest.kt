package ca.stewark.nocturnel.visualizer

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerWiringTest {
    @Test fun mediaServiceUsesPcmTeeAndNoCapturePermission() {
        val service = File("src/main/java/ca/stewark/nocturnel/playback/NocturneLPlaybackService.kt").readText()
        val app = File("src/main/java/ca/stewark/nocturnel/NocturneLApplication.kt").readText()
        val factory = File("src/main/java/ca/stewark/nocturnel/visualizer/VisualizerRenderersFactory.kt").readText()
        val connection = File("src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt").readText()
        val ui = File("src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt").readText()
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue("VisualizerRenderersFactory" in service)
        assertTrue("setPlaybackActive(isPlaying)" in service)
        assertTrue("audioAnalysis: AudioAnalysisRepository" in app)
        assertTrue("setAudioProcessors(arrayOf(teeAudioProcessor))" in factory)
        assertTrue("val analysisState = app.audioAnalysis.state" in connection)
        assertTrue("onVisualizerActiveChanged = playback::setVisualizerActive" in ui)
        assertTrue("fun setVisualizerSyncOffsetMs(offsetMs: Int)" in connection)
        assertTrue("app.audioAnalysis.setVisualizerSyncOffsetMs(offsetMs)" in connection)
        assertTrue("LaunchedEffect(settings.visualizerSyncOffsetMs)" in ui)
        assertTrue("playback.setVisualizerSyncOffsetMs(settings.visualizerSyncOffsetMs)" in ui)
        assertTrue("visualizerSyncOffsetMs = settings.visualizerSyncOffsetMs" in ui)
        assertTrue("onDecreaseVisualizerSyncOffset = settingsViewModel::decreaseVisualizerSyncOffset" in ui)
        assertTrue("onIncreaseVisualizerSyncOffset = settingsViewModel::increaseVisualizerSyncOffset" in ui)
        assertTrue("onResetVisualizerSyncOffset = settingsViewModel::resetVisualizerSyncOffset" in ui)
        assertFalse("visualizerSyncOffsetMs:" in connection.substringAfter("data class PlaybackUiState"))
        assertFalse("RECORD_AUDIO" in manifest)
        assertFalse("MODIFY_AUDIO_SETTINGS" in manifest)
    }
}
