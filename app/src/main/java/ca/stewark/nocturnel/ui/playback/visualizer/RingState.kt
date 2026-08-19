package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame

internal data class RingEchoState(val age: Int, val intensity: Float)

internal data class RingState(
    val frameId: Long,
    val magnitudes: List<Float>,
    val previousMagnitudes: List<Float>,
    val echo: RingEchoState?,
) {
    companion object {
        val Empty = RingState(Long.MIN_VALUE, emptyList(), emptyList(), null)
    }
}

internal fun updateRingState(
    state: RingState,
    mode: VisualizerDisplayMode,
    frame: AudioAnalysisFrame,
    effectsEnabled: Boolean,
): RingState {
    if (mode != VisualizerDisplayMode.RING || frame.status != AnalysisStatus.ACTIVE) return RingState.Empty

    val current = ringMagnitudes(frame, RING_STATE_MAGNITUDE_COUNT)
    val increasing = state.frameId != Long.MIN_VALUE && frame.frameId > state.frameId
    val smoothed = if (increasing && state.magnitudes.size == current.size) {
        current.indices.map { index -> state.magnitudes[index] * .35f + current[index] * .65f }
    } else {
        current
    }
    val transient = frame.transient.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val echo = when {
        !effectsEnabled -> null
        transient >= RING_ECHO_THRESHOLD -> RingEchoState(0, transient)
        !increasing -> null
        state.echo != null && state.echo.age < RING_ECHO_LAST_AGE -> state.echo.copy(age = state.echo.age + 1)
        else -> null
    }
    return RingState(
        frameId = frame.frameId,
        magnitudes = smoothed,
        previousMagnitudes = if (increasing) state.magnitudes else emptyList(),
        echo = echo,
    )
}

private const val RING_STATE_MAGNITUDE_COUNT = 96
private const val RING_ECHO_THRESHOLD = .65f
private const val RING_ECHO_LAST_AGE = 3
