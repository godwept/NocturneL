package ca.stewark.nocturnel.ui.components

import kotlin.math.max
import kotlin.math.min

internal data class DragReorderSession(
    val draggedKey: String,
    val startingOrder: List<String>,
    val previewOrder: List<String>,
    val startIndex: Int,
    val targetIndex: Int,
    val translationY: Float = 0f,
)

internal data class DragReorderItemBounds(
    val key: String,
    val previewIndex: Int,
    val top: Float,
    val bottom: Float,
) {
    val midpoint: Float get() = (top + bottom) / 2f
}

internal data class DragReorderCommit(
    val key: String,
    val targetIndex: Int,
)

internal fun beginDragReorder(order: List<String>, draggedKey: String): DragReorderSession? {
    val startIndex = order.indexOf(draggedKey)
    if (startIndex < 0) return null
    return DragReorderSession(
        draggedKey = draggedKey,
        startingOrder = order.toList(),
        previewOrder = order.toList(),
        startIndex = startIndex,
        targetIndex = startIndex,
    )
}

internal fun DragReorderSession.moveTo(
    targetIndex: Int,
    translationCorrection: Float = 0f,
): DragReorderSession {
    if (previewOrder.isEmpty()) return this
    val clampedTarget = targetIndex.coerceIn(0, previewOrder.lastIndex)
    val reordered = previewOrder.toMutableList()
    val currentIndex = reordered.indexOf(draggedKey)
    if (currentIndex < 0) return this
    reordered.removeAt(currentIndex)
    reordered.add(clampedTarget, draggedKey)
    return copy(
        previewOrder = reordered,
        targetIndex = clampedTarget,
        translationY = translationY + translationCorrection,
    )
}

internal fun dragReorderTargetIndex(
    draggedKey: String,
    draggedCenterY: Float,
    visibleItems: List<DragReorderItemBounds>,
    currentTargetIndex: Int,
): Int {
    val neighbors = visibleItems.filter { it.key != draggedKey }
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

internal fun dragReorderEdgeVelocity(
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
        draggedCenterY < topEdgeEnd ->
            -(((topEdgeEnd - draggedCenterY) / edgeSize).coerceIn(0f, 1f) * maximumVelocity)
        draggedCenterY > bottomEdgeStart ->
            ((draggedCenterY - bottomEdgeStart) / edgeSize).coerceIn(0f, 1f) * maximumVelocity
        else -> 0f
    }
}

internal fun DragReorderSession.isCompatible(authoritativeOrder: List<String>): Boolean =
    startingOrder == authoritativeOrder && draggedKey in authoritativeOrder

internal fun DragReorderSession.commitOrNull(): DragReorderCommit? =
    if (targetIndex == startIndex) null else DragReorderCommit(draggedKey, targetIndex)
