package ca.stewark.nocturnel.playback

import ca.stewark.nocturnel.data.entity.TrackEntity

data class QueueAddition(val tracks: List<TrackEntity>, val skipped: Int, val message: String)

fun queueAddition(tracks: List<TrackEntity>, externallySkipped: Int = 0): QueueAddition {
    val playable = tracks.filter { it.status == "PLAYABLE" }
    val skipped = externallySkipped + tracks.size - playable.size
    val message = when {
        playable.isEmpty() -> "NO PLAYABLE TRACKS"
        skipped > 0 -> "QUEUED ${playable.size} TRACK(S) · SKIPPED $skipped"
        else -> "QUEUED ${playable.size} TRACK(S)"
    }
    return QueueAddition(playable, skipped, message)
}

class PendingQueueActions<T> {
    private val actions = ArrayDeque<T>()
    fun add(action: T) { actions.addLast(action) }
    fun drain(): List<T> = buildList { while (actions.isNotEmpty()) add(actions.removeFirst()) }
    fun clear() = actions.clear()
}
