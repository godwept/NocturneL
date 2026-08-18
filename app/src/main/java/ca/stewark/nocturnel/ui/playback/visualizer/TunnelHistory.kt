package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame

internal data class TunnelHistory(
    val priorFrames: List<AudioAnalysisFrame>,
    val currentFrame: AudioAnalysisFrame?,
) {
    companion object {
        val Empty = TunnelHistory(emptyList(), null)
    }
}

internal fun updateTunnelHistory(
    history: TunnelHistory,
    mode: VisualizerDisplayMode,
    frame: AudioAnalysisFrame,
    effectsEnabled: Boolean,
): TunnelHistory {
    if (mode != VisualizerDisplayMode.TUNNEL || frame.status != AnalysisStatus.ACTIVE || !effectsEnabled) {
        return TunnelHistory.Empty
    }
    val current = history.currentFrame ?: return TunnelHistory(emptyList(), frame)
    if (frame.frameId <= current.frameId) return TunnelHistory(emptyList(), frame)
    return TunnelHistory((history.priorFrames + current).takeLast(MAX_TUNNEL_HISTORY), frame)
}

private const val MAX_TUNNEL_HISTORY = 3
