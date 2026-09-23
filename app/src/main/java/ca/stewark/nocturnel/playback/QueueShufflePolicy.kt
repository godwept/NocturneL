package ca.stewark.nocturnel.playback

object QueueShufflePolicy {
    fun forNewQueue(
        snapshot: QueueSnapshot,
        shuffleEnabled: Boolean,
        shuffler: (List<QueueEntry>) -> List<QueueEntry> = { it.shuffled() },
    ): QueueSnapshot {
        val ordered = snapshot.copy(shuffle = false)
        return if (shuffleEnabled) toggle(ordered, shuffler) else ordered
    }

    fun toggle(
        snapshot: QueueSnapshot,
        shuffler: (List<QueueEntry>) -> List<QueueEntry> = { it.shuffled() },
    ): QueueSnapshot {
        if (snapshot.shuffle) return snapshot.copy(shuffle = false)
        val split = (snapshot.currentIndex + 1).coerceIn(0, snapshot.entries.size)
        val upcoming = snapshot.entries.drop(split)
        val shuffled = shuffler(upcoming)
            .takeIf { it.size == upcoming.size && it.toSet() == upcoming.toSet() }
            ?: upcoming
        return snapshot.copy(entries = snapshot.entries.take(split) + shuffled, shuffle = true)
    }
}
