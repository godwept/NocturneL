package ca.stewark.nocturnel.ui.playback

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.BracketIconButton
import ca.stewark.nocturnel.ui.components.DragReorderHandle
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.components.dragReorderRow
import ca.stewark.nocturnel.ui.components.rememberDragReorderLazyListState
import ca.stewark.nocturnel.ui.library.formatDuration
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun QueueEditorScreen(
    state: QueueEditorState,
    onBack: () -> Unit,
    onJump: (String) -> Unit,
    onMove: (String, Int, String?) -> Unit,
    onRemove: (String) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onExpireUndo: () -> Unit,
) {
    var confirmingClear by rememberSaveable { mutableStateOf(false) }
    val authoritativeOrder = state.upcoming.map { it.track.occurrenceId }
    val dragState = rememberDragReorderLazyListState(authoritativeOrder)
    val currentOccurrenceId = state.current?.occurrenceId
    var expectedCurrentOccurrenceId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentOccurrenceId) {
        if (dragState.isDragging && expectedCurrentOccurrenceId != currentOccurrenceId) {
            dragState.cancel()
            expectedCurrentOccurrenceId = null
        }
    }

    val rowsById = state.upcoming.associateBy { it.track.occurrenceId }
    val displayedRows = (dragState.previewOrder ?: authoritativeOrder).mapIndexedNotNull { index, id ->
        rowsById[id]?.copy(
            upcomingIndex = index,
            canMoveUp = index > 0,
            canMoveDown = index < state.upcoming.lastIndex,
        )
    }

    Column(Modifier.fillMaxSize().padding(TerminalDimensions.sm)) {
        Row {
            BracketButton("BACK", { onExpireUndo(); onBack() })
            if (!confirmingClear) {
                BracketButton("CLEAR UPCOMING", { confirmingClear = true }, enabled = state.canClear)
            } else {
                BracketButton("CONFIRM CLEAR", { confirmingClear = false; onClear() })
                BracketButton("CANCEL", { confirmingClear = false })
            }
        }
        AsciiFrame("CURRENT", Modifier.padding(top = TerminalDimensions.xs)) {
            val current = state.current
            if (current == null) Text("NO TRACK SELECTED") else {
                Text(current.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${current.artist} · ${current.album}",
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        state.notice?.let { TerminalNotice(it, Modifier.padding(vertical = TerminalDimensions.xs)) }
        if (state.canUndo) BracketButton("UNDO", onUndo)
        Text("+--[ UPCOMING ]", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = TerminalDimensions.sm))
        if (displayedRows.isEmpty()) {
            TerminalNotice("QUEUE EMPTY")
        } else {
            LazyColumn(state = dragState.listState, userScrollEnabled = !dragState.isDragging) {
                items(displayedRows, key = { it.track.occurrenceId }) { row ->
                    val session = dragState.activeSession
                    val isDragging = session?.draggedKey == row.track.occurrenceId
                    UpcomingQueueRow(
                        row = row,
                        currentOccurrenceId = currentOccurrenceId,
                        isDragging = isDragging,
                        dragTranslationY = if (isDragging) session.translationY else 0f,
                        itemCount = displayedRows.size,
                        onJump = onJump,
                        onMove = onMove,
                        onRemove = onRemove,
                        onDragStart = { occurrenceId ->
                            expectedCurrentOccurrenceId = currentOccurrenceId
                            dragState.start(authoritativeOrder, occurrenceId)
                        },
                        onDrag = dragState::dragBy,
                        onDragEnd = {
                            val expectedCurrent = expectedCurrentOccurrenceId
                            val commit = dragState.finish(authoritativeOrder)
                            expectedCurrentOccurrenceId = null
                            if (expectedCurrent == currentOccurrenceId) {
                                commit?.let { onMove(it.key, it.targetIndex, expectedCurrent) }
                            }
                        },
                        onDragCancel = {
                            dragState.cancel()
                            expectedCurrentOccurrenceId = null
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun UpcomingQueueRow(
    row: QueueEditorRow,
    currentOccurrenceId: String?,
    isDragging: Boolean,
    dragTranslationY: Float,
    itemCount: Int,
    onJump: (String) -> Unit,
    onMove: (String, Int, String?) -> Unit,
    onRemove: (String) -> Unit,
    onDragStart: (String) -> Unit,
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
                position = row.upcomingIndex,
                itemCount = itemCount,
                testTag = "queue-row-${row.track.occurrenceId}",
            )
            .padding(vertical = TerminalDimensions.xs),
    ) {
        DragReorderHandle(
            title = row.track.title,
            dragKey = row.track.occurrenceId,
            testTag = "queue-drag-${row.track.occurrenceId}",
            canMoveUp = row.canMoveUp,
            canMoveDown = row.canMoveDown,
            onMoveUp = { onMove(row.track.occurrenceId, row.upcomingIndex - 1, currentOccurrenceId) },
            onMoveDown = { onMove(row.track.occurrenceId, row.upcomingIndex + 1, currentOccurrenceId) },
            onDragStart = { onDragStart(row.track.occurrenceId) },
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
        )
        Column(Modifier.weight(1f).padding(horizontal = TerminalDimensions.xs)) {
            Text(row.track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${row.track.artist} · ${formatDuration(row.track.durationMs)}",
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BracketIconButton(">", "Jump to ${row.track.title}", { onJump(row.track.occurrenceId) })
        BracketIconButton("X", "Remove ${row.track.title}", { onRemove(row.track.occurrenceId) })
    }
}
