package ca.stewark.nocturnel.playback

class QueueController(private val state: QueueState) {
    fun next(isPlayable: (String) -> Boolean): QueueState {
        if (state.paths.isEmpty()) return state
        if (state.repeat == RepeatMode.ONE && state.currentIndex >= 0) return state
        val start = if (state.currentIndex < 0) 0 else state.currentIndex + 1
        val candidates = (start until state.paths.size) + if (state.repeat == RepeatMode.ALL) (0 until start.coerceAtMost(state.paths.size)) else emptyList()
        val index = candidates.firstOrNull { isPlayable(state.paths[it]) } ?: return state
        return state.copy(currentIndex = index)
    }

    fun previous(isPlayable: (String) -> Boolean): QueueState {
        if (state.paths.isEmpty()) return state
        val start = if (state.currentIndex <= 0) state.paths.lastIndex else state.currentIndex - 1
        val candidates = (start downTo 0) + if (state.repeat == RepeatMode.ALL) (state.paths.lastIndex downTo (start + 1)) else emptyList()
        val index = candidates.firstOrNull { isPlayable(state.paths[it]) } ?: return state
        return state.copy(currentIndex = index)
    }
}
