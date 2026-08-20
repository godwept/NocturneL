package ca.stewark.nocturnel.visualizer

object VisualizerSyncOffset {
    const val DEFAULT_MS = 0
    const val MIN_MS = -2_000
    const val MAX_MS = 2_000
    const val STEP_MS = 25

    fun clamp(offsetMs: Int): Int = offsetMs.coerceIn(MIN_MS, MAX_MS)

    fun increase(offsetMs: Int): Int = clamp(offsetMs + STEP_MS)

    fun decrease(offsetMs: Int): Int = clamp(offsetMs - STEP_MS)

    fun label(offsetMs: Int): String {
        val clamped = clamp(offsetMs)
        return if (clamped > 0) "+$clamped ms" else "$clamped ms"
    }
}
