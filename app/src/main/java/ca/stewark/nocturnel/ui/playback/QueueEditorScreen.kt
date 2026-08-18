package ca.stewark.nocturnel.ui.playback

import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.BracketIconButton
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.library.formatDuration
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import kotlin.math.abs

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
                Text(current.title)
                Text("${current.artist} · ${current.album}", color = MaterialTheme.colorScheme.secondary)
            }
        }
        state.notice?.let { TerminalNotice(it, Modifier.padding(vertical = TerminalDimensions.xs)) }
        if (state.canUndo) BracketButton("UNDO", onUndo)
        Text("+--[ UPCOMING ]", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = TerminalDimensions.sm))
        if (state.upcoming.isEmpty()) {
            TerminalNotice("QUEUE EMPTY")
        } else {
            LazyColumn {
                items(state.upcoming, key = { it.track.occurrenceId }) { row ->
                    UpcomingQueueRow(
                        row = row,
                        currentOccurrenceId = state.current?.occurrenceId,
                        onJump = onJump,
                        onMove = onMove,
                        onRemove = onRemove,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingQueueRow(
    row: QueueEditorRow,
    currentOccurrenceId: String?,
    onJump: (String) -> Unit,
    onMove: (String, Int, String?) -> Unit,
    onRemove: (String) -> Unit,
) {
    var rowHeight by remember { mutableIntStateOf(1) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    val actions = buildList {
        if (row.canMoveUp) add(CustomAccessibilityAction("Move ${row.track.title} up") {
            onMove(row.track.occurrenceId, row.upcomingIndex - 1, currentOccurrenceId); true
        })
        if (row.canMoveDown) add(CustomAccessibilityAction("Move ${row.track.title} down") {
            onMove(row.track.occurrenceId, row.upcomingIndex + 1, currentOccurrenceId); true
        })
    }
    Row(
        Modifier.fillMaxWidth().onSizeChanged { rowHeight = it.height.coerceAtLeast(1) }
            .padding(vertical = TerminalDimensions.xs),
    ) {
        BracketIconButton(
            "::",
            "Reorder ${row.track.title}",
            {},
            Modifier.testTag("queue-drag-${row.track.occurrenceId}")
                .semantics { customActions = actions }
                .pointerInput(row.track.occurrenceId, currentOccurrenceId) {
                    detectVerticalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            dragDistance += amount
                            if (abs(dragDistance) >= rowHeight) {
                                val target = row.upcomingIndex + if (dragDistance > 0) 1 else -1
                                onMove(row.track.occurrenceId, target, currentOccurrenceId)
                                dragDistance = 0f
                            }
                        },
                    )
                },
        )
        Column(Modifier.weight(1f).padding(horizontal = TerminalDimensions.xs)) {
            Text(row.track.title)
            Text("${row.track.artist} · ${formatDuration(row.track.durationMs)}", color = MaterialTheme.colorScheme.secondary)
        }
        BracketIconButton(">", "Jump to ${row.track.title}", { onJump(row.track.occurrenceId) })
        BracketIconButton("X", "Remove ${row.track.title}", { onRemove(row.track.occurrenceId) })
    }
}
