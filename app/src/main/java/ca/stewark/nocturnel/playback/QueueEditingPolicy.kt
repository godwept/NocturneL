package ca.stewark.nocturnel.playback

const val QUEUE_OCCURRENCE_ID = "queue_occurrence_id"
const val QUEUE_DURATION_MS = "duration_ms"
const val QUEUE_ALBUM_ID = "album_id"

data class QueueEntry(
    val occurrenceId: String,
    val relativePath: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
)

data class QueueSnapshot(
    val entries: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
)

sealed interface QueueEditCommand {
    data class Append(val entries: List<QueueEntry>) : QueueEditCommand
    data class Move(val occurrenceId: String, val targetUpcomingIndex: Int) : QueueEditCommand
    data class Remove(val occurrenceId: String) : QueueEditCommand
    data object ClearUpcoming : QueueEditCommand
    data class RestoreRemoved(val token: QueueUndoToken) : QueueEditCommand
}

data class QueueUndoToken(val entry: QueueEntry, val upcomingIndex: Int)

data class QueueEditResult(
    val snapshot: QueueSnapshot,
    val removed: QueueUndoToken? = null,
    val changed: Boolean,
    val notice: String? = null,
)

object QueueEditingPolicy {
    fun apply(snapshot: QueueSnapshot, command: QueueEditCommand): QueueEditResult = when (command) {
        is QueueEditCommand.Append -> append(snapshot, command.entries)
        is QueueEditCommand.Move -> move(snapshot, command.occurrenceId, command.targetUpcomingIndex)
        is QueueEditCommand.Remove -> remove(snapshot, command.occurrenceId)
        QueueEditCommand.ClearUpcoming -> clear(snapshot)
        is QueueEditCommand.RestoreRemoved -> restore(snapshot, command.token)
    }

    private fun append(snapshot: QueueSnapshot, additions: List<QueueEntry>): QueueEditResult {
        if (additions.isEmpty()) return unchanged(snapshot)
        return changed(snapshot, snapshot.entries + additions)
    }

    private fun move(snapshot: QueueSnapshot, occurrenceId: String, targetUpcomingIndex: Int): QueueEditResult {
        val split = (snapshot.currentIndex + 1).coerceIn(0, snapshot.entries.size)
        val upcoming = snapshot.entries.drop(split).toMutableList()
        val from = upcoming.indexOfFirst { it.occurrenceId == occurrenceId }
        if (from < 0) return unchanged(snapshot, "QUEUE CHANGED · TRY AGAIN")
        val target = targetUpcomingIndex.coerceIn(upcoming.indices)
        if (from == target) return unchanged(snapshot)
        val entry = upcoming.removeAt(from)
        upcoming.add(target, entry)
        return changed(snapshot, snapshot.entries.take(split) + upcoming)
    }

    private fun remove(snapshot: QueueSnapshot, occurrenceId: String): QueueEditResult {
        val split = (snapshot.currentIndex + 1).coerceIn(0, snapshot.entries.size)
        val upcoming = snapshot.entries.drop(split).toMutableList()
        val index = upcoming.indexOfFirst { it.occurrenceId == occurrenceId }
        if (index < 0) return unchanged(snapshot, "QUEUE CHANGED · TRY AGAIN")
        val removed = upcoming.removeAt(index)
        val result = changed(snapshot, snapshot.entries.take(split) + upcoming, "REMOVED ${removed.title.uppercase()}")
        return result.copy(removed = QueueUndoToken(removed, index))
    }

    private fun restore(snapshot: QueueSnapshot, token: QueueUndoToken): QueueEditResult {
        val split = (snapshot.currentIndex + 1).coerceIn(0, snapshot.entries.size)
        val upcoming = snapshot.entries.drop(split).toMutableList()
        upcoming.add(token.upcomingIndex.coerceIn(0, upcoming.size), token.entry)
        return changed(snapshot, snapshot.entries.take(split) + upcoming, "QUEUE ITEM RESTORED")
    }

    private fun clear(snapshot: QueueSnapshot): QueueEditResult {
        val split = (snapshot.currentIndex + 1).coerceIn(0, snapshot.entries.size)
        if (split >= snapshot.entries.size) return unchanged(snapshot)
        val repeat = if (snapshot.repeat == RepeatMode.ALL) RepeatMode.OFF else snapshot.repeat
        val notice = if (snapshot.repeat == RepeatMode.ALL) "UPCOMING CLEARED · REPEAT ALL DISABLED" else "UPCOMING QUEUE CLEARED"
        return QueueEditResult(
            snapshot.copy(entries = snapshot.entries.take(split), shuffle = false, repeat = repeat),
            changed = true,
            notice = notice,
        )
    }

    private fun changed(snapshot: QueueSnapshot, entries: List<QueueEntry>, notice: String? = null): QueueEditResult {
        val modeNotice = if (snapshot.shuffle) "SHUFFLE DISABLED · QUEUE UPDATED" else notice
        return QueueEditResult(snapshot.copy(entries = entries, shuffle = false), changed = true, notice = modeNotice)
    }

    private fun unchanged(snapshot: QueueSnapshot, notice: String? = null) = QueueEditResult(snapshot, changed = false, notice = notice)
}
