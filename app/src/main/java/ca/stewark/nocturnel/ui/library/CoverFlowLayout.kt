package ca.stewark.nocturnel.ui.library

import kotlin.math.abs

internal const val COVER_FLOW_WIDTH_FRACTION = 0.84f
internal const val COVER_FLOW_MAX_SIZE = 340f
internal const val COVER_FLOW_NEIGHBOR_SCALE = 0.76f
internal const val COVER_FLOW_NEIGHBOR_ALPHA = 0.5f
internal const val COVER_FLOW_NEIGHBOR_EXPOSURE = 0.15f

internal data class CoverFlowVisualState(
    val scale: Float,
    val alpha: Float,
    val stackingOrder: Float,
    val interactive: Boolean,
)

internal fun coverFlowCoverSize(availableWidth: Float, availableHeight: Float): Float =
    minOf(
        availableWidth.coerceAtLeast(0f) * COVER_FLOW_WIDTH_FRACTION,
        availableHeight.coerceAtLeast(0f),
        COVER_FLOW_MAX_SIZE,
    )

internal fun coverFlowItemStride(
    coverSize: Float,
    neighborScale: Float = COVER_FLOW_NEIGHBOR_SCALE,
    exposedFraction: Float = COVER_FLOW_NEIGHBOR_EXPOSURE,
): Float {
    val size = coverSize.coerceAtLeast(0f)
    val scale = neighborScale.coerceIn(0f, 1f)
    val exposure = exposedFraction.coerceIn(0f, 1f)
    val neighborWidth = size * scale
    return (size / 2f - neighborWidth / 2f + neighborWidth * exposure).coerceAtLeast(0f)
}

internal fun coverFlowItemSpacing(coverSize: Float): Float =
    coverFlowItemStride(coverSize) - coverSize.coerceAtLeast(0f)

internal fun coverFlowDistanceFromCenter(
    viewportStart: Int,
    viewportEnd: Int,
    itemOffset: Int,
    itemSize: Int,
    itemStride: Float,
): Float {
    if (itemStride <= 0f) return 0f
    val viewportCenter = (viewportStart + viewportEnd) / 2f
    val itemCenter = itemOffset + itemSize / 2f
    return (itemCenter - viewportCenter) / itemStride
}

internal fun coverFlowVisualState(distanceFromCenter: Float): CoverFlowVisualState {
    val distance = abs(distanceFromCenter)
    val centerProximity = (1f - distance).coerceIn(0f, 1f)
    val scale = COVER_FLOW_NEIGHBOR_SCALE +
        (1f - COVER_FLOW_NEIGHBOR_SCALE) * centerProximity
    val alpha = if (distance <= 1f) {
        COVER_FLOW_NEIGHBOR_ALPHA +
            (1f - COVER_FLOW_NEIGHBOR_ALPHA) * centerProximity
    } else {
        COVER_FLOW_NEIGHBOR_ALPHA * (2f - distance).coerceIn(0f, 1f)
    }
    return CoverFlowVisualState(
        scale = scale,
        alpha = alpha,
        stackingOrder = (2f - distance).coerceAtLeast(0f),
        interactive = distance < 2f,
    )
}
