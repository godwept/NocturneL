package ca.stewark.nocturnel.playback

data class PlaybackOccurrenceProgress(
    val relativePath: String,
    val occurrenceId: String,
    val accumulatedListeningMs: Long = 0,
    val qualified: Boolean = false,
)

data class PlaybackQualification(val qualificationId: String, val relativePath: String)

class PlaybackProgressTracker(initial: List<PlaybackOccurrenceProgress> = emptyList()) {
    private val states = initial.associateBy { it.occurrenceId }.toMutableMap()
    private val emitted = initial.filter { it.qualified }.mapTo(mutableSetOf()) { it.occurrenceId }
    private var currentOccurrenceId: String? = null
    private var currentDurationMs: Long = 0
    private var active = false
    private var lastTickMs: Long? = null

    fun update(
        occurrenceId: String,
        relativePath: String,
        durationMs: Long,
        isPlaying: Boolean,
        nowElapsedMs: Long,
    ): PlaybackQualification? {
        accrue(nowElapsedMs)
        val previousQualification = currentOccurrenceId?.let(::qualifyIfReady)
        if (currentOccurrenceId != occurrenceId) {
            currentOccurrenceId = occurrenceId
            states.putIfAbsent(occurrenceId, PlaybackOccurrenceProgress(relativePath, occurrenceId))
        }
        currentDurationMs = durationMs
        active = isPlaying
        lastTickMs = nowElapsedMs
        return previousQualification ?: qualifyIfReady(occurrenceId)
    }

    fun discontinuity(nowElapsedMs: Long) {
        accrue(nowElapsedMs)
        lastTickMs = nowElapsedMs
    }

    fun complete(nowElapsedMs: Long): PlaybackQualification? {
        accrue(nowElapsedMs)
        active = false
        val occurrenceId = currentOccurrenceId ?: return null
        return if (currentDurationMs <= 0) qualify(occurrenceId) else qualifyIfReady(occurrenceId)
    }

    fun markQualified(occurrenceId: String) {
        states[occurrenceId]?.let { states[occurrenceId] = it.copy(qualified = true) }
        emitted += occurrenceId
    }

    fun recordFailed(occurrenceId: String) { emitted -= occurrenceId }

    fun snapshot(): List<PlaybackOccurrenceProgress> = states.values.toList()

    private fun accrue(nowElapsedMs: Long) {
        val occurrenceId = currentOccurrenceId
        val previous = lastTickMs
        if (active && occurrenceId != null && previous != null && nowElapsedMs >= previous) {
            states[occurrenceId]?.let {
                states[occurrenceId] = it.copy(accumulatedListeningMs = it.accumulatedListeningMs + nowElapsedMs - previous)
            }
        }
    }

    private fun qualifyIfReady(occurrenceId: String): PlaybackQualification? {
        val state = states[occurrenceId] ?: return null
        if (state.qualified || occurrenceId in emitted) return null
        val threshold = if (currentDurationMs > 0) minOf(currentDurationMs / 2, FOUR_MINUTES_MS) else FOUR_MINUTES_MS
        return if (state.accumulatedListeningMs >= threshold) {
            emitted += occurrenceId
            PlaybackQualification(occurrenceId, state.relativePath)
        } else null
    }

    private fun qualify(occurrenceId: String): PlaybackQualification? {
        val state = states[occurrenceId] ?: return null
        return if (state.qualified || occurrenceId in emitted) null else {
            emitted += occurrenceId
            PlaybackQualification(occurrenceId, state.relativePath)
        }
    }

    private companion object { const val FOUR_MINUTES_MS = 240_000L }
}
