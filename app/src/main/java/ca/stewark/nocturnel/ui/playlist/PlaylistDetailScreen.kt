package ca.stewark.nocturnel.ui.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.BracketIconButton
import ca.stewark.nocturnel.ui.components.DragReorderHandle
import ca.stewark.nocturnel.ui.components.TerminalTextField
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.ui.components.dragReorderRow
import ca.stewark.nocturnel.ui.components.rememberDragReorderLazyListState
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun PlaylistDetailScreen(
    state: PlaylistDetailState,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onRename: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onAddToQueue: (List<TrackEntity>, Int) -> Unit = { _, _ -> },
) {
    var adding by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var name by remember(state.playlist.id, state.playlist.name) { mutableStateOf(state.playlist.name) }
    val playableTracks = state.entries.mapNotNull { it.track }
    val skippedTracks = state.entries.size - playableTracks.size
    val authoritativeOrder = state.entries.map { it.dragKey }
    val dragState = rememberDragReorderLazyListState(authoritativeOrder)
    val rowsByKey = state.entries.associateBy { it.dragKey }
    val displayedRows = (dragState.previewOrder ?: authoritativeOrder).mapIndexedNotNull { index, key ->
        rowsByKey[key]?.let { it to index }
    }
    Column(Modifier.fillMaxSize().padding(TerminalDimensions.sm)) {
        Row { BracketButton("BACK", onBack) }
        AsciiFrame(state.playlist.name) {
            TerminalTextField(name, { name = it }, "PLAYLIST NAME")
            Row {
                val actionStyle = MaterialTheme.typography.labelMedium
                BracketButton("PLAY", onPlay, enabled = state.entries.any { it.available }, textStyle = actionStyle)
                BracketButton("RENAME", { onRename(name) }, enabled = name.isNotBlank() && name != state.playlist.name, textStyle = actionStyle)
                BracketButton(if (adding) "CLOSE ADD" else "ADD TRACK", { adding = !adding }, textStyle = actionStyle)
                BracketButton(
                    "ADD QUEUE",
                    { onAddToQueue(playableTracks, skippedTracks) },
                    enabled = playableTracks.isNotEmpty(),
                    textStyle = actionStyle,
                )
            }
        }
        LazyColumn(state = dragState.listState, userScrollEnabled = !dragState.isDragging) {
            if (adding) {
                item { TerminalTextField(query, { query = it }, "FILTER AVAILABLE TRACKS", Modifier.padding(vertical = TerminalDimensions.xs)) }
                items(
                    state.availableTracks.filter { "${it.title} ${it.artist} ${it.album}".contains(query, true) },
                    key = { "available:${it.relativePath}" },
                ) { track ->
                    Row(Modifier.fillMaxWidth()) {
                        BracketIconButton("+", "Add ${track.title}", { onAdd(track.relativePath) })
                        Text(
                            "${track.artist} :: ${track.title}",
                            Modifier.weight(1f).padding(top = TerminalDimensions.sm),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            items(displayedRows, key = { it.first.dragKey }) { (row, previewIndex) ->
                val session = dragState.activeSession
                val isDragging = session?.draggedKey == row.dragKey
                PlaylistTrackEntryRow(
                    row = row,
                    previewIndex = previewIndex,
                    itemCount = displayedRows.size,
                    isDragging = isDragging,
                    dragTranslationY = if (isDragging) session.translationY else 0f,
                    onMove = onMove,
                    onRemove = onRemove,
                    onDragStart = { dragState.start(authoritativeOrder, row.dragKey) },
                    onDrag = dragState::dragBy,
                    onDragEnd = {
                        dragState.finish(authoritativeOrder)?.let { commit ->
                            rowsByKey[commit.key]?.let { moved -> onMove(moved.position, commit.targetIndex) }
                        }
                    },
                    onDragCancel = dragState::cancel,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

private val PlaylistTrackRow.dragKey: String get() = "$position:$relativePath"

@Composable
internal fun PlaylistTrackEntryRow(
    row: PlaylistTrackRow,
    previewIndex: Int,
    itemCount: Int,
    isDragging: Boolean,
    dragTranslationY: Float,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .dragReorderRow(
                isDragging = isDragging,
                translationY = dragTranslationY,
                position = previewIndex,
                itemCount = itemCount,
                testTag = "playlist-row-${row.position}",
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DragReorderHandle(
            title = row.title,
            dragKey = row.dragKey,
            testTag = "playlist-drag-${row.position}",
            canMoveUp = previewIndex > 0,
            canMoveDown = previewIndex < itemCount - 1,
            onMoveUp = { onMove(row.position, previewIndex - 1) },
            onMoveDown = { onMove(row.position, previewIndex + 1) },
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
        )
        Text(
            "${row.artist} :: ${row.title}",
            Modifier.weight(1f).padding(horizontal = TerminalDimensions.xs),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        BracketIconButton("X", "Remove ${row.title}", { onRemove(row.position) })
    }
}
