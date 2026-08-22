package ca.stewark.nocturnel.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.ui.artwork.RetroArtwork
import ca.stewark.nocturnel.ui.components.FavoriteToggle
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import ca.stewark.nocturnel.ui.theme.TerminalTheme
import ca.stewark.nocturnel.ui.components.terminalBorder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun AlbumCoverFlowScreen(
    albums: List<AlbumEntity>,
    state: LazyListState,
    favoriteAlbumIds: Set<String>,
    albumPlayCounts: Map<String, Long>,
    effectsEnabled: Boolean,
    selectedAlbumId: String?,
    onSelectedAlbumChanged: (String?) -> Unit,
    onAlbumSelected: (AlbumEntity) -> Unit,
    onToggleFavorite: (AlbumEntity) -> Unit,
) {
    if (albums.isEmpty()) return
    val palette = TerminalTheme.palette

    val albumIds = albums.map { it.id }
    val selectedIndex = albumIds.indexOf(selectedAlbumId).takeIf { it >= 0 }
        ?: state.firstVisibleItemIndex.coerceIn(albums.indices)
    val selectedAlbum = albums[selectedIndex]
    val scope = rememberCoroutineScope()

    LaunchedEffect(albumIds) {
        val reconciled = reconcileCoverFlowSelection(
            previousAlbumId = selectedAlbumId,
            previousIndex = selectedIndex,
            albumIds = albumIds,
        )
        if (reconciled.albumId != selectedAlbumId) onSelectedAlbumChanged(reconciled.albumId)
        reconciled.index?.let { if (it != state.firstVisibleItemIndex) state.scrollToItem(it) }
    }
    LaunchedEffect(state, albumIds, selectedAlbumId) {
        snapshotFlow {
            val layout = state.layoutInfo
            nearestCoverIndex(
                layout.viewportStartOffset,
                layout.viewportEndOffset,
                layout.visibleItemsInfo.map { CoverFlowItemBounds(it.index, it.offset, it.size) },
            )
        }.distinctUntilChanged().collect { index ->
            index?.takeIf { it in albums.indices }?.let { centeredIndex ->
                val centeredId = albums[centeredIndex].id
                if (centeredId != selectedAlbumId) onSelectedAlbumChanged(centeredId)
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .testTag(if (effectsEnabled) "animated-cover-flow" else "static-cover-flow"),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val coverSize = minOf(maxWidth * 0.62f, 240.dp)
            val sidePadding = ((maxWidth - coverSize) / 2).coerceAtLeast(0.dp)
            LazyRow(
                state = state,
                modifier = Modifier.fillMaxSize().testTag("cover-flow-reel"),
                contentPadding = PaddingValues(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(TerminalDimensions.sm),
                verticalAlignment = Alignment.CenterVertically,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            ) {
                itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
                    val selected = album.id == selectedAlbum.id
                    val targetScale = if (selected) 1f else 0.72f
                    val targetAlpha = if (selected) 1f else 0.5f
                    val scale = if (effectsEnabled) {
                        animateFloatAsState(targetScale, label = "cover scale").value
                    } else {
                        targetScale
                    }
                    val alpha = if (effectsEnabled) {
                        animateFloatAsState(targetAlpha, label = "cover alpha").value
                    } else {
                        targetAlpha
                    }
                    val description = buildString {
                        if (selected) append("Selected ")
                        append(album.title)
                        append(", ${index + 1} of ${albums.size}")
                    }
                    Box(
                        Modifier
                            .size(coverSize)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .alpha(alpha)
                            .terminalBorder(
                                width = if (selected) 2.dp else TerminalDimensions.border,
                                color = if (selected) palette.selection else palette.border,
                                emphasized = selected,
                            )
                            .semantics { contentDescription = description }
                            .testTag("cover-flow-cover-${album.id}")
                            .clickable {
                                if (selected) {
                                    onAlbumSelected(album)
                                } else {
                                    scope.launch {
                                        if (effectsEnabled) state.animateScrollToItem(index)
                                        else state.scrollToItem(index)
                                    }
                                }
                            }
                            .padding(TerminalDimensions.xs),
                    ) {
                        RetroArtwork(album, Modifier.fillMaxWidth().aspectRatio(1f))
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = TerminalDimensions.md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("> CURRENT_", color = palette.selection)
                Text(positionLabel(selectedIndex, albums.size), color = MaterialTheme.colorScheme.secondary)
            }
            Text(
                selectedAlbum.title.uppercase(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                selectedAlbum.artist,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${albumPlayCounts[selectedAlbum.id] ?: 0}×",
                    color = MaterialTheme.colorScheme.secondary,
                )
                FavoriteToggle(
                    title = selectedAlbum.title,
                    selected = selectedAlbum.id in favoriteAlbumIds,
                    onToggle = { onToggleFavorite(selectedAlbum) },
                )
            }
        }
    }
}

private fun positionLabel(index: Int, size: Int): String =
    "${(index + 1).toString().padStart(2, '0')} / ${size.toString().padStart(2, '0')}"
