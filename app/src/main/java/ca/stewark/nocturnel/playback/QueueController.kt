package ca.stewark.nocturnel.playback

class QueueController(private val state: QueueState) {
    fun next(isPlayable: (String) -> Boolean): QueueState {
        if (state.paths.isEmpty()) return state
        if (state.repeat == RepeatMode.ONE && state.currentIndex >= 0) return state
        val order = order()
        val currentOrderIndex = order.indexOf(state.currentIndex)
        val start = if (currentOrderIndex < 0) 0 else currentOrderIndex + 1
        val candidates = order.drop(start) + if (state.repeat == RepeatMode.ALL) order.take(start) else emptyList()
        val index = candidates.firstOrNull { isPlayable(state.paths[it]) } ?: return state
        return state.copy(currentIndex = index)
    }

    fun previous(isPlayable: (String) -> Boolean): QueueState {
        if (state.paths.isEmpty()) return state
        if (state.repeat == RepeatMode.ONE && state.currentIndex >= 0) return state
        val order = order()
        val currentOrderIndex = order.indexOf(state.currentIndex)
        val before = if (currentOrderIndex < 0) order.indices else 0 until currentOrderIndex
        val after = if (currentOrderIndex < 0) emptyList() else (currentOrderIndex + 1 until order.size).toList()
        val candidates = before.reversed().map(order::get) +
            if (state.repeat == RepeatMode.ALL) after.reversed().map(order::get) else emptyList()
        val index = candidates.firstOrNull { isPlayable(state.paths[it]) } ?: return state
        return state.copy(currentIndex = index)
    }

    fun seekTo(index: Int, isPlayable: (String) -> Boolean): QueueState {
        if (index !in state.paths.indices || !isPlayable(state.paths[index])) return state
        return state.copy(currentIndex = index)
    }

    fun withRepeat(repeat: RepeatMode): QueueState = state.copy(repeat = repeat)

    fun withShuffle(enabled: Boolean, shuffler: (List<Int>) -> List<Int> = { it.shuffled() }): QueueState {
        if (!enabled) return state.copy(shuffle = false, playOrder = emptyList())
        val indices = state.paths.indices.toList()
        if (indices.isEmpty()) return state.copy(shuffle = true, playOrder = emptyList())
        val current = state.currentIndex.takeIf { it in state.paths.indices }
        val remaining = indices.filterNot { it == current }
        val shuffled = shuffler(remaining).takeIf { it.size == remaining.size && it.toSet() == remaining.toSet() } ?: remaining
        return state.copy(shuffle = true, playOrder = listOfNotNull(current) + shuffled)
    }

    private fun order(): List<Int> =
        state.playOrder.takeIf { state.shuffle && it.size == state.paths.size && it.toSet() == state.paths.indices.toSet() }
            ?: state.paths.indices.toList()

    companion object {
        fun start(paths: List<String>, startIndex: Int = 0, isPlayable: (String) -> Boolean): QueueState {
            if (paths.isEmpty()) return QueueState()
            val normalizedStart = startIndex.coerceIn(paths.indices)
            val candidates = (normalizedStart until paths.size) + (0 until normalizedStart)
            return QueueState(paths, candidates.firstOrNull { isPlayable(paths[it]) } ?: -1)
        }
    }
}
