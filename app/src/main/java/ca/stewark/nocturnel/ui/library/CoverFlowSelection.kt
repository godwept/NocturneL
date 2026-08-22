package ca.stewark.nocturnel.ui.library

internal data class CoverFlowItemBounds(
    val index: Int,
    val offset: Int,
    val size: Int,
)

internal data class CoverFlowSelection(
    val albumId: String?,
    val index: Int?,
)

internal fun nearestCoverIndex(
    viewportStart: Int,
    viewportEnd: Int,
    visibleItems: List<CoverFlowItemBounds>,
): Int? {
    val viewportCenter = (viewportStart + viewportEnd) / 2f
    return visibleItems.minWithOrNull(
        compareBy<CoverFlowItemBounds> {
            kotlin.math.abs((it.offset + it.size / 2f) - viewportCenter)
        }.thenBy { it.index },
    )?.index
}

internal fun reconcileCoverFlowSelection(
    previousAlbumId: String?,
    previousIndex: Int?,
    albumIds: List<String>,
): CoverFlowSelection {
    if (albumIds.isEmpty()) return CoverFlowSelection(null, null)
    val retainedIndex = previousAlbumId?.let(albumIds::indexOf)?.takeIf { it >= 0 }
    val index = retainedIndex ?: (previousIndex ?: 0).coerceIn(albumIds.indices)
    return CoverFlowSelection(albumIds[index], index)
}
