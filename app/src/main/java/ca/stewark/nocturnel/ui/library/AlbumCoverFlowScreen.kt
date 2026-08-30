package ca.stewark.nocturnel.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
            val coverSize = coverFlowCoverSize(maxWidth.value, maxHeight.value).dp
            val sidePadding = ((maxWidth - coverSize) / 2).coerceAtLeast(0.dp)
            val itemSpacing = coverFlowItemSpacing(coverSize.value).dp
            val itemStridePx = with(LocalDensity.current) {
                coverFlowItemStride(coverSize.value).dp.toPx()
            }
            LazyRow(
                state = state,
                modifier = Modifier.fillMaxSize().testTag("cover-flow-reel"),
                contentPadding = PaddingValues(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalAlignment = Alignment.CenterVertically,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            ) {
                itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
                    val selected = album.id == selectedAlbum.id
                    val visualState by remember(state, index, selectedIndex, itemStridePx) {
                        derivedStateOf {
                            val layout = state.layoutInfo
                            val item = layout.visibleItemsInfo.firstOrNull { it.index == index }
                            val distance = item?.let {
                                coverFlowDistanceFromCenter(
                                    viewportStart = layout.viewportStartOffset,
                                    viewportEnd = layout.viewportEndOffset,
                                    itemOffset = it.offset,
                                    itemSize = it.size,
                                    itemStride = itemStridePx,
                                )
                            } ?: (index - selectedIndex).toFloat()
                            coverFlowVisualState(distance)
                        }
                    }
                    val scale = if (effectsEnabled) {
                        animateFloatAsState(visualState.scale, label = "cover scale").value
                    } else {
                        visualState.scale
                    }
                    val alpha = if (effectsEnabled) {
                        animateFloatAsState(visualState.alpha, label = "cover alpha").value
                    } else {
                        visualState.alpha
                    }
                    val description = buildString {
                        if (selected) append("Selected ")
                        append(album.title)
                        append(", ${index + 1} of ${albums.size}")
                    }
                    val activateCover: () -> Unit = {
                        if (selected) {
                            onAlbumSelected(album)
                        } else {
                            scope.launch {
                                if (effectsEnabled) state.animateScrollToItem(index)
                                else state.scrollToItem(index)
                            }
                        }
                        Unit
                    }
                    Box(
                        Modifier
                            .size(coverSize)
                            .zIndex(visualState.stackingOrder)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                            .background(MaterialTheme.colorScheme.background)
                            .terminalBorder(
                                width = if (selected) 2.dp else TerminalDimensions.border,
                                color = if (selected) palette.selection else palette.border,
                                emphasized = selected,
                            )
                            .then(
                                if (visualState.interactive) {
                                    Modifier
                                        .semantics { contentDescription = description }
                                        .testTag("cover-flow-cover-${album.id}")
                                        .clickable(onClick = activateCover)
                                } else {
                                    Modifier
                                        .clearAndSetSemantics { }
                                        .testTag("cover-flow-cover-${album.id}")
                                },
                            )
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
