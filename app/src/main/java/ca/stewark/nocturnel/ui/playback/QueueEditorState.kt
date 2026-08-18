package ca.stewark.nocturnel.ui.playback

import ca.stewark.nocturnel.playback.QueueEntry
import ca.stewark.nocturnel.playback.PlaybackUiState

data class QueueEditorTrack(
    val occurrenceId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
)

data class QueueEditorRow(
    val track: QueueEditorTrack,
    val upcomingIndex: Int,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
)

data class QueueEditorState(
    val current: QueueEditorTrack?,
    val upcoming: List<QueueEditorRow>,
    val canClear: Boolean,
    val canUndo: Boolean,
    val notice: String?,
)

fun queueEditorState(
    current: QueueEntry?,
    upcoming: List<QueueEntry>,
    canUndo: Boolean,
    notice: String?,
): QueueEditorState = QueueEditorState(
    current = current?.toEditorTrack(),
    upcoming = upcoming.mapIndexed { index, entry ->
        QueueEditorRow(entry.toEditorTrack(), index, index > 0, index < upcoming.lastIndex)
    },
    canClear = upcoming.isNotEmpty(),
    canUndo = canUndo,
    notice = notice,
)

private fun QueueEntry.toEditorTrack() = QueueEditorTrack(occurrenceId, title, artist, album, durationMs)

fun PlaybackUiState.toQueueEditorState(): QueueEditorState {
    val current = if (currentOccurrenceId != null && currentPath != null) {
        QueueEntry(currentOccurrenceId, currentPath, title.orEmpty(), artist.orEmpty(), album.orEmpty(), durationMs)
    } else null
    return queueEditorState(current, upNext.map { it.toQueueEntry() }, canUndoQueueRemoval, queueNotice)
}
