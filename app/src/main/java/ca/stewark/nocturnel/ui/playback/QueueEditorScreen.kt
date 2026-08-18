package ca.stewark.nocturnel.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.BracketIconButton
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.library.formatDuration
import ca.stewark.nocturnel.ui.theme.AlertAmber
import ca.stewark.nocturnel.ui.theme.TerminalBlackAlt
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import kotlin.math.abs

private val QueueDragEdgeSize = 64.dp
private val QueueDragMaximumVelocity = 900.dp

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
    val listState = rememberLazyListState()
    var dragSession by remember { mutableStateOf<QueueDragSession?>(null) }
    var edgeVelocity by remember { mutableFloatStateOf(0f) }
    val authoritativeOrder = state.upcoming.map { it.track.occurrenceId }
    val currentOccurrenceId = state.current?.occurrenceId
    val density = LocalDensity.current
    val edgeSizePx = with(density) { QueueDragEdgeSize.toPx() }
    val maximumVelocityPx = with(density) { QueueDragMaximumVelocity.toPx() }

    fun visibleBounds(): List<QueueDragItemBounds> = listState.layoutInfo.visibleItemsInfo.mapNotNull { item ->
        val occurrenceId = item.key as? String ?: return@mapNotNull null
        QueueDragItemBounds(
            occurrenceId = occurrenceId,
            previewIndex = item.index,
            top = item.offset.toFloat(),
            bottom = (item.offset + item.size).toFloat(),
        )
    }

    fun updateEdgeVelocity(session: QueueDragSession) {
        val draggedItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == session.draggedOccurrenceId }
        edgeVelocity = if (draggedItem == null) 0f else queueDragEdgeVelocity(
            draggedCenterY = draggedItem.offset + draggedItem.size / 2f + session.translationY,
            viewportStart = listState.layoutInfo.viewportStartOffset.toFloat(),
            viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat(),
            edgeSize = edgeSizePx,
            maximumVelocity = maximumVelocityPx,
        )
    }

    fun retarget(session: QueueDragSession): QueueDragSession {
        val info = listState.layoutInfo.visibleItemsInfo
        val draggedItem = info.firstOrNull { it.key == session.draggedOccurrenceId } ?: return session
        val center = draggedItem.offset + draggedItem.size / 2f + session.translationY
        val target = queueDragTargetIndex(
            draggedOccurrenceId = session.draggedOccurrenceId,
            draggedCenterY = center,
            visibleItems = visibleBounds(),
            currentTargetIndex = session.targetIndex,
        )
        if (target == session.targetIndex) return session
        val targetItem = info.firstOrNull { it.index == target } ?: return session.moveTo(target)
        return session.moveTo(target, (draggedItem.offset - targetItem.offset).toFloat())
    }

    fun cancelDrag() {
        dragSession = null
        edgeVelocity = 0f
    }

    fun dragBy(amount: Float) {
        val session = dragSession ?: return
        val retargeted = retarget(session.copy(translationY = session.translationY + amount))
        dragSession = retargeted
        updateEdgeVelocity(retargeted)
    }

    fun finishDrag() {
        val session = dragSession
        cancelDrag()
        if (session != null && session.isCompatible(authoritativeOrder, currentOccurrenceId)) {
            session.commitOrNull()?.let { commit ->
                onMove(commit.occurrenceId, commit.targetIndex, commit.expectedCurrentOccurrenceId)
            }
        }
    }

    LaunchedEffect(authoritativeOrder, currentOccurrenceId) {
        val session = dragSession
        if (session != null && !session.isCompatible(authoritativeOrder, currentOccurrenceId)) cancelDrag()
    }

    LaunchedEffect(dragSession?.draggedOccurrenceId, edgeVelocity) {
        if (dragSession == null || edgeVelocity == 0f) return@LaunchedEffect
        var previousFrameNanos = 0L
        while (dragSession != null && edgeVelocity != 0f) {
            val frameNanos = withFrameNanos { it }
            if (previousFrameNanos != 0L) {
                val seconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
                val consumed = listState.scrollBy(edgeVelocity * seconds)
                val session = dragSession
                if (session != null && abs(consumed) > 0.01f) {
                    val retargeted = retarget(session.copy(translationY = session.translationY + consumed))
                    dragSession = retargeted
                    updateEdgeVelocity(retargeted)
                } else if (abs(consumed) <= 0.01f) {
                    edgeVelocity = 0f
                }
            }
            previousFrameNanos = frameNanos
        }
    }

    val rowsById = state.upcoming.associateBy { it.track.occurrenceId }
    val displayedRows = (dragSession?.previewOrder ?: authoritativeOrder).mapIndexedNotNull { index, id ->
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
                Text(current.title)
                Text("${current.artist} · ${current.album}", color = MaterialTheme.colorScheme.secondary)
            }
        }
        state.notice?.let { TerminalNotice(it, Modifier.padding(vertical = TerminalDimensions.xs)) }
        if (state.canUndo) BracketButton("UNDO", onUndo)
        Text("+--[ UPCOMING ]", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = TerminalDimensions.sm))
        if (displayedRows.isEmpty()) {
            TerminalNotice("QUEUE EMPTY")
        } else {
            LazyColumn(state = listState, userScrollEnabled = dragSession == null) {
                items(displayedRows, key = { it.track.occurrenceId }) { row ->
                    val session = dragSession
                    val isDragging = session?.draggedOccurrenceId == row.track.occurrenceId
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
                            dragSession = beginQueueDrag(authoritativeOrder, occurrenceId, currentOccurrenceId)
                            dragSession?.let(::updateEdgeVelocity)
                        },
                        onDrag = ::dragBy,
                        onDragEnd = ::finishDrag,
                        onDragCancel = ::cancelDrag,
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
    val actions = buildList {
        if (row.canMoveUp) add(CustomAccessibilityAction("Move ${row.track.title} up") {
            onMove(row.track.occurrenceId, row.upcomingIndex - 1, currentOccurrenceId); true
        })
        if (row.canMoveDown) add(CustomAccessibilityAction("Move ${row.track.title} down") {
            onMove(row.track.occurrenceId, row.upcomingIndex + 1, currentOccurrenceId); true
        })
    }
    Row(
        modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = dragTranslationY
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
                shadowElevation = if (isDragging) 8.dp.toPx() else 0f
            }
            .then(
                if (isDragging) Modifier.background(TerminalBlackAlt).border(TerminalDimensions.border, AlertAmber)
                else Modifier,
            )
            .testTag("queue-row-${row.track.occurrenceId}")
            .semantics {
                if (isDragging) stateDescription = "Dragging, position ${row.upcomingIndex + 1} of $itemCount"
            }
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
                        onDragStart = { onDragStart(row.track.occurrenceId) },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            onDrag(amount)
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
