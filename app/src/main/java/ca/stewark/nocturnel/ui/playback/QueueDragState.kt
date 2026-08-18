package ca.stewark.nocturnel.ui.playback

import kotlin.math.max
import kotlin.math.min

internal data class QueueDragSession(
    val draggedOccurrenceId: String,
    val startingOrder: List<String>,
    val previewOrder: List<String>,
    val startIndex: Int,
    val targetIndex: Int,
    val expectedCurrentOccurrenceId: String?,
    val translationY: Float = 0f,
)

internal data class QueueDragItemBounds(
    val occurrenceId: String,
    val previewIndex: Int,
    val top: Float,
    val bottom: Float,
) {
    val midpoint: Float get() = (top + bottom) / 2f
}

internal data class QueueDragCommit(
    val occurrenceId: String,
    val targetIndex: Int,
    val expectedCurrentOccurrenceId: String?,
)

internal fun beginQueueDrag(
    occurrenceOrder: List<String>,
    draggedOccurrenceId: String,
    expectedCurrentOccurrenceId: String?,
): QueueDragSession? {
    val startIndex = occurrenceOrder.indexOf(draggedOccurrenceId)
    if (startIndex < 0) return null
    return QueueDragSession(
        draggedOccurrenceId = draggedOccurrenceId,
        startingOrder = occurrenceOrder.toList(),
        previewOrder = occurrenceOrder.toList(),
        startIndex = startIndex,
        targetIndex = startIndex,
        expectedCurrentOccurrenceId = expectedCurrentOccurrenceId,
    )
}

internal fun QueueDragSession.moveTo(targetIndex: Int, translationCorrection: Float = 0f): QueueDragSession {
    if (previewOrder.isEmpty()) return this
    val clampedTarget = targetIndex.coerceIn(0, previewOrder.lastIndex)
    val reordered = previewOrder.toMutableList()
    val currentIndex = reordered.indexOf(draggedOccurrenceId)
    if (currentIndex < 0) return this
    reordered.removeAt(currentIndex)
    reordered.add(clampedTarget, draggedOccurrenceId)
    return copy(
        previewOrder = reordered,
        targetIndex = clampedTarget,
        translationY = translationY + translationCorrection,
    )
}

internal fun queueDragTargetIndex(
    draggedOccurrenceId: String,
    draggedCenterY: Float,
    visibleItems: List<QueueDragItemBounds>,
    currentTargetIndex: Int,
): Int {
    val neighbors = visibleItems.filter { it.occurrenceId != draggedOccurrenceId }
    if (neighbors.isEmpty()) return currentTargetIndex

    var target = currentTargetIndex
    neighbors.forEach { item ->
        if (item.previewIndex > currentTargetIndex && draggedCenterY > item.midpoint) {
            target = max(target, item.previewIndex)
        } else if (item.previewIndex < currentTargetIndex && draggedCenterY < item.midpoint) {
            target = min(target, item.previewIndex)
        }
    }
    return target
}

internal fun queueDragEdgeVelocity(
    draggedCenterY: Float,
    viewportStart: Float,
    viewportEnd: Float,
    edgeSize: Float,
    maximumVelocity: Float,
): Float {
    if (edgeSize <= 0f || maximumVelocity <= 0f || viewportEnd <= viewportStart) return 0f
    val topEdgeEnd = viewportStart + edgeSize
    val bottomEdgeStart = viewportEnd - edgeSize
    return when {
        draggedCenterY < topEdgeEnd -> {
            -(((topEdgeEnd - draggedCenterY) / edgeSize).coerceIn(0f, 1f) * maximumVelocity)
        }
        draggedCenterY > bottomEdgeStart -> {
            ((draggedCenterY - bottomEdgeStart) / edgeSize).coerceIn(0f, 1f) * maximumVelocity
        }
        else -> 0f
    }
}

internal fun QueueDragSession.isCompatible(
    authoritativeOrder: List<String>,
    currentOccurrenceId: String?,
): Boolean = startingOrder == authoritativeOrder &&
    expectedCurrentOccurrenceId == currentOccurrenceId &&
    draggedOccurrenceId in authoritativeOrder

internal fun QueueDragSession.commitOrNull(): QueueDragCommit? =
    if (targetIndex == startIndex) null else QueueDragCommit(
        occurrenceId = draggedOccurrenceId,
        targetIndex = targetIndex,
        expectedCurrentOccurrenceId = expectedCurrentOccurrenceId,
    )
