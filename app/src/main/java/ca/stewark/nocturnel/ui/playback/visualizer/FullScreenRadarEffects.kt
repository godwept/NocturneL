package ca.stewark.nocturnel.ui.playback.visualizer

internal const val RADAR_FULL_SCREEN_PULSE_DURATION_NANOS = 750_000_000L
internal const val RADAR_FULL_SCREEN_MAX_PULSES = 4

internal const val RADAR_EXTENDED_BEAM_ALPHA = .14f
internal const val RADAR_EXTENDED_BEAM_WIDTH = 2.5f
internal const val RADAR_EXTENDED_WAKE_ALPHA_SCALE = .10f
internal const val RADAR_EXTENDED_WAKE_WIDTH = 6f
internal const val RADAR_PULSE_EDGE_MAX_ALPHA = .24f
internal const val RADAR_PULSE_BODY_MAX_ALPHA = .10f
internal const val RADAR_PULSE_EDGE_WIDTH = 5f
internal const val RADAR_PULSE_BODY_WIDTH = 26f

internal data class RadarFullScreenPulse(
    val strength: Float,
    val ageNanos: Long,
)

internal data class RadarFullScreenEffectState(
    val previousTransient: Float = 0f,
    val pulses: List<RadarFullScreenPulse> = emptyList(),
) {
    companion object {
        val Empty = RadarFullScreenEffectState()
    }
}

internal fun updateRadarFullScreenEffects(
    state: RadarFullScreenEffectState,
    transient: Float,
    elapsedNanos: Long,
): RadarFullScreenEffectState {
    val elapsed = elapsedNanos.coerceAtLeast(0L)
    val current = sanitizeRadarTransient(transient)
    val aged = state.pulses
        .map { it.copy(ageNanos = saturatedRadarPulseAge(it.ageNanos, elapsed)) }
        .filter { it.ageNanos < RADAR_FULL_SCREEN_PULSE_DURATION_NANOS }

    val pulses = if (current > state.previousTransient) {
        (aged + RadarFullScreenPulse(current, 0L)).takeLast(RADAR_FULL_SCREEN_MAX_PULSES)
    } else {
        aged
    }
    return RadarFullScreenEffectState(previousTransient = current, pulses = pulses)
}

internal fun radarFullScreenPulseProgress(pulse: RadarFullScreenPulse): Float =
    (pulse.ageNanos.coerceAtLeast(0L).toFloat() / RADAR_FULL_SCREEN_PULSE_DURATION_NANOS.toFloat())
        .coerceIn(0f, 1f)

internal fun radarFullScreenPulseAlpha(pulse: RadarFullScreenPulse, maxAlpha: Float): Float {
    val strength = sanitizeRadarTransient(pulse.strength)
    val alphaLimit = if (maxAlpha.isFinite()) maxAlpha.coerceIn(0f, 1f) else 0f
    return strength * alphaLimit * (1f - radarFullScreenPulseProgress(pulse))
}

private fun sanitizeRadarTransient(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else 0f

private fun saturatedRadarPulseAge(left: Long, right: Long): Long =
    if (Long.MAX_VALUE - left < right) Long.MAX_VALUE else left + right
