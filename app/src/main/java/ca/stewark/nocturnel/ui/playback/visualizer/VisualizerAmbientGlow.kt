package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame

internal fun ambientGlowAlpha(
    mode: VisualizerDisplayMode,
    frame: AudioAnalysisFrame,
    effectsEnabled: Boolean,
): Float {
    if (!effectsEnabled || frame.status != AnalysisStatus.ACTIVE ||
        (mode != VisualizerDisplayMode.RADAR && mode != VisualizerDisplayMode.GRID)) return 0f
    val transient = frame.transient
    return if (transient.isFinite()) transient.coerceIn(0f, 1f) * .24f else 0f
}
