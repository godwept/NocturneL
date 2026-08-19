package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val DragReorderEdgeSize = 64.dp
private val DragReorderMaximumVelocity = 900.dp

@Stable
internal class DragReorderLazyListState(
    val listState: LazyListState,
    private val edgeSizePx: Float,
    private val maximumVelocityPx: Float,
) {
    var activeSession by mutableStateOf<DragReorderSession?>(null)
        private set
    var edgeVelocity by mutableFloatStateOf(0f)
        private set

    val previewOrder: List<String>? get() = activeSession?.previewOrder
    val isDragging: Boolean get() = activeSession != null

    fun start(authoritativeOrder: List<String>, key: String) {
        activeSession = beginDragReorder(authoritativeOrder, key)
        activeSession?.let(::updateEdgeVelocity)
    }

    fun dragBy(amount: Float) {
        val session = activeSession ?: return
        val retargeted = retarget(session.copy(translationY = session.translationY + amount))
        activeSession = retargeted
        updateEdgeVelocity(retargeted)
    }

    fun finish(authoritativeOrder: List<String>): DragReorderCommit? {
        val session = activeSession
        cancel()
        return session?.takeIf { it.isCompatible(authoritativeOrder) }?.commitOrNull()
    }

    fun cancel() {
        activeSession = null
        edgeVelocity = 0f
    }

    fun cancelIfIncompatible(authoritativeOrder: List<String>) {
        val session = activeSession
        if (session != null && !session.isCompatible(authoritativeOrder)) cancel()
    }

    suspend fun autoScrollFrame(seconds: Float): Boolean {
        val velocity = edgeVelocity
        if (activeSession == null || velocity == 0f) return false
        val consumed = listState.scrollBy(velocity * seconds)
        val session = activeSession
        if (session != null && abs(consumed) > 0.01f) {
            val retargeted = retarget(session.copy(translationY = session.translationY + consumed))
            activeSession = retargeted
            updateEdgeVelocity(retargeted)
            return true
        }
        if (abs(consumed) <= 0.01f) edgeVelocity = 0f
        return false
    }

    private fun visibleBounds(): List<DragReorderItemBounds> =
        listState.layoutInfo.visibleItemsInfo.mapNotNull { item ->
            val key = item.key as? String ?: return@mapNotNull null
            val previewIndex = activeSession?.previewOrder?.indexOf(key) ?: return@mapNotNull null
            if (previewIndex < 0) return@mapNotNull null
            DragReorderItemBounds(
                key = key,
                previewIndex = previewIndex,
                top = item.offset.toFloat(),
                bottom = (item.offset + item.size).toFloat(),
            )
        }

    private fun retarget(session: DragReorderSession): DragReorderSession {
        val info = listState.layoutInfo.visibleItemsInfo
        val draggedItem = info.firstOrNull { it.key == session.draggedKey } ?: return session
        val center = draggedItem.offset + draggedItem.size / 2f + session.translationY
        val target = dragReorderTargetIndex(
            draggedKey = session.draggedKey,
            draggedCenterY = center,
            visibleItems = visibleBounds(),
            currentTargetIndex = session.targetIndex,
        )
        if (target == session.targetIndex) return session
        val targetKey = session.previewOrder.getOrNull(target) ?: return session.moveTo(target)
        val targetItem = info.firstOrNull { it.key == targetKey } ?: return session.moveTo(target)
        return session.moveTo(target, (draggedItem.offset - targetItem.offset).toFloat())
    }

    private fun updateEdgeVelocity(session: DragReorderSession) {
        val draggedItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == session.draggedKey }
        edgeVelocity = if (draggedItem == null) 0f else dragReorderEdgeVelocity(
            draggedCenterY = draggedItem.offset + draggedItem.size / 2f + session.translationY,
            viewportStart = listState.layoutInfo.viewportStartOffset.toFloat(),
            viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat(),
            edgeSize = edgeSizePx,
            maximumVelocity = maximumVelocityPx,
        )
    }
}

@Composable
internal fun rememberDragReorderLazyListState(
    authoritativeOrder: List<String>,
): DragReorderLazyListState {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val edgeSizePx = with(density) { DragReorderEdgeSize.toPx() }
    val maximumVelocityPx = with(density) { DragReorderMaximumVelocity.toPx() }
    val state = remember(listState, edgeSizePx, maximumVelocityPx) {
        DragReorderLazyListState(listState, edgeSizePx, maximumVelocityPx)
    }

    LaunchedEffect(authoritativeOrder) {
        state.cancelIfIncompatible(authoritativeOrder)
    }
    LaunchedEffect(state.activeSession?.draggedKey, state.edgeVelocity) {
        if (!state.isDragging || state.edgeVelocity == 0f) return@LaunchedEffect
        var previousFrameNanos = 0L
        while (state.isDragging && state.edgeVelocity != 0f) {
            val frameNanos = withFrameNanos { it }
            if (previousFrameNanos != 0L) {
                val seconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
                state.autoScrollFrame(seconds)
            }
            previousFrameNanos = frameNanos
        }
    }
    return state
}
