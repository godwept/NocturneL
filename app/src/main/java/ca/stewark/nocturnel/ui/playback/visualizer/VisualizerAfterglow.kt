package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import kotlin.math.max

internal const val AFTERGLOW_DURATION_NANOS = 250_000_000L
internal const val RADAR_AFTERGLOW_MAX_ALPHA = .28f
internal const val BAND_AFTERGLOW_MAX_ALPHA = .30f

private const val MAX_RADAR_AFTERGLOW_SAMPLES = 8

internal data class VisualizerSizeKey(val width: Int, val height: Int)

internal data class RadarAfterglowSample(
    val angleDegrees: Float,
    val ageNanos: Long,
) {
    val alpha: Float
        get() = decayAlpha(RADAR_AFTERGLOW_MAX_ALPHA, ageNanos)
}

internal data class RadarAfterglowState(
    val currentFrameId: Long? = null,
    val currentAngleDegrees: Float = 0f,
    val samples: List<RadarAfterglowSample> = emptyList(),
)

internal data class BandAfterglow(
    val decayStartLevel: Float,
    val retainedLevel: Float,
    val ageNanos: Long,
) {
    val alpha: Float
        get() = if (ageNanos == 0L) 0f else decayAlpha(BAND_AFTERGLOW_MAX_ALPHA, ageNanos)
}

internal data class VisualizerAfterglowState(
    val activeMode: VisualizerDisplayMode? = null,
    val size: VisualizerSizeKey? = null,
    val lastFrameId: Long? = null,
    val radar: RadarAfterglowState = RadarAfterglowState(),
    val bands: List<BandAfterglow> = emptyList(),
) {
    companion object {
        val Empty = VisualizerAfterglowState()
    }
}

internal fun updateRadarAfterglow(
    state: RadarAfterglowState,
    frameId: Long,
    angleDegrees: Float,
    elapsedNanos: Long,
): RadarAfterglowState {
    val elapsed = elapsedNanos.coerceAtLeast(0L)
    val normalizedAngle = normalizeDegrees(angleDegrees)
    val currentId = state.currentFrameId
    if (currentId == null || frameId < currentId || elapsed >= AFTERGLOW_DURATION_NANOS) {
        return RadarAfterglowState(frameId, normalizedAngle)
    }

    val aged = state.samples
        .map { it.copy(ageNanos = saturatedAdd(it.ageNanos, elapsed)) }
        .filter { it.ageNanos < AFTERGLOW_DURATION_NANOS }

    if (frameId == currentId) {
        return state.copy(currentAngleDegrees = normalizedAngle, samples = aged)
    }

    val samples = (aged + RadarAfterglowSample(state.currentAngleDegrees, 0L))
        .takeLast(MAX_RADAR_AFTERGLOW_SAMPLES)
    return RadarAfterglowState(frameId, normalizedAngle, samples)
}

internal fun updateBandAfterglow(
    state: List<BandAfterglow>,
    liveLevels: List<Float>,
    elapsedNanos: Long,
): List<BandAfterglow> {
    val sanitized = liveLevels.map(::sanitizeLevel)
    if (state.size != sanitized.size) return sanitized.map(::baselineBand)

    val elapsed = elapsedNanos.coerceAtLeast(0L)
    return state.zip(sanitized) { previous, live ->
        if (live >= previous.retainedLevel) {
            baselineBand(live)
        } else {
            val startingDecay = previous.ageNanos == 0L
            val startLevel = if (startingDecay) previous.retainedLevel else previous.decayStartLevel
            val age = saturatedAdd(if (startingDecay) 0L else previous.ageNanos, elapsed)
            if (age >= AFTERGLOW_DURATION_NANOS) {
                baselineBand(live)
            } else {
                val progress = age.toFloat() / AFTERGLOW_DURATION_NANOS.toFloat()
                BandAfterglow(
                    decayStartLevel = startLevel,
                    retainedLevel = max(live, startLevel * (1f - progress)),
                    ageNanos = age,
                )
            }
        }
    }
}

internal fun updateVisualizerAfterglow(
    state: VisualizerAfterglowState,
    mode: VisualizerDisplayMode,
    frame: AudioAnalysisFrame,
    effectsEnabled: Boolean,
    size: VisualizerSizeKey,
    elapsedNanos: Long,
): VisualizerAfterglowState {
    val eligible = effectsEnabled &&
        frame.status == AnalysisStatus.ACTIVE &&
        mode != VisualizerDisplayMode.ART &&
        size.width > 0 &&
        size.height > 0
    if (!eligible) return VisualizerAfterglowState.Empty

    val lifecycleChanged = state.activeMode != mode || state.size != size
    val frameRewound = !lifecycleChanged && state.lastFrameId?.let { frame.frameId < it } == true
    val base = if (lifecycleChanged || frameRewound) VisualizerAfterglowState.Empty else state

    return when (mode) {
        VisualizerDisplayMode.RADAR -> VisualizerAfterglowState(
            activeMode = mode,
            size = size,
            lastFrameId = frame.frameId,
            radar = updateRadarAfterglow(
                base.radar,
                frame.frameId,
                normalizeDegrees(frame.frameId * 2f),
                elapsedNanos,
            ),
        )
        VisualizerDisplayMode.BANDS -> VisualizerAfterglowState(
            activeMode = mode,
            size = size,
            lastFrameId = frame.frameId,
            bands = updateBandAfterglow(base.bands, frame.bands, elapsedNanos),
        )
        VisualizerDisplayMode.ART -> VisualizerAfterglowState.Empty
    }
}

private fun baselineBand(level: Float) = BandAfterglow(level, level, 0L)

private fun sanitizeLevel(value: Float): Float = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f

private fun normalizeDegrees(value: Float): Float {
    if (!value.isFinite()) return 0f
    return ((value % 360f) + 360f) % 360f
}

private fun decayAlpha(maxAlpha: Float, ageNanos: Long): Float {
    val progress = (ageNanos.coerceAtLeast(0L).toFloat() / AFTERGLOW_DURATION_NANOS.toFloat()).coerceIn(0f, 1f)
    return maxAlpha * (1f - progress)
}

private fun saturatedAdd(left: Long, right: Long): Long =
    if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
