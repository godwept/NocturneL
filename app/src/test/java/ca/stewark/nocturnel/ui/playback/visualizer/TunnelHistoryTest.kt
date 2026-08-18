package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import org.junit.Assert.assertEquals
import org.junit.Test

class TunnelHistoryTest {
    @Test fun retainsOnlyThreePriorIncreasingFrames() {
        var history = updateTunnelHistory(TunnelHistory.Empty, VisualizerDisplayMode.TUNNEL, frame(1), true)
        assertEquals(emptyList<AudioAnalysisFrame>(), history.priorFrames)
        assertEquals(1L, history.currentFrame?.frameId)

        for (id in 2L..5L) {
            history = updateTunnelHistory(history, VisualizerDisplayMode.TUNNEL, frame(id), true)
        }

        assertEquals(listOf(2L, 3L, 4L), history.priorFrames.map { it.frameId })
        assertEquals(5L, history.currentFrame?.frameId)
    }

    @Test fun clearsBeforeAcceptingNonIncreasingFrame() {
        var history = updateTunnelHistory(TunnelHistory.Empty, VisualizerDisplayMode.TUNNEL, frame(4), true)
        history = updateTunnelHistory(history, VisualizerDisplayMode.TUNNEL, frame(5), true)
        history = updateTunnelHistory(history, VisualizerDisplayMode.TUNNEL, frame(3), true)

        assertEquals(emptyList<AudioAnalysisFrame>(), history.priorFrames)
        assertEquals(3L, history.currentFrame?.frameId)
    }

    @Test fun clearsOutsideActiveEffectsEnabledTunnel() {
        val populated = updateTunnelHistory(
            updateTunnelHistory(TunnelHistory.Empty, VisualizerDisplayMode.TUNNEL, frame(1), true),
            VisualizerDisplayMode.TUNNEL,
            frame(2),
            true,
        )
        listOf(VisualizerDisplayMode.ART, VisualizerDisplayMode.RADAR, VisualizerDisplayMode.BANDS).forEach { mode ->
            assertEquals(TunnelHistory.Empty, updateTunnelHistory(populated, mode, frame(3), true))
        }
        assertEquals(TunnelHistory.Empty, updateTunnelHistory(populated, VisualizerDisplayMode.TUNNEL, AudioAnalysisFrame.Idle, true))
        assertEquals(TunnelHistory.Empty, updateTunnelHistory(populated, VisualizerDisplayMode.TUNNEL, AudioAnalysisFrame.Unavailable, true))
        assertEquals(TunnelHistory.Empty, updateTunnelHistory(populated, VisualizerDisplayMode.TUNNEL, frame(3), false))
        val reentered = updateTunnelHistory(TunnelHistory.Empty, VisualizerDisplayMode.TUNNEL, frame(9), true)
        assertEquals(emptyList<AudioAnalysisFrame>(), reentered.priorFrames)
        assertEquals(9L, reentered.currentFrame?.frameId)
    }

    private fun frame(id: Long) = AudioAnalysisFrame(
        waveform = List(128) { 0f },
        bands = List(32) { 0f },
        energy = 0f,
        lowEnergy = 0f,
        midEnergy = 0f,
        highEnergy = 0f,
        transient = 0f,
        frameId = id,
        status = AnalysisStatus.ACTIVE,
    )
}
