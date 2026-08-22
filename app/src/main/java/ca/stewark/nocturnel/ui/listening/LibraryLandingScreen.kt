package ca.stewark.nocturnel.ui.listening

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.library.AlbumCoverFlowScreen
import ca.stewark.nocturnel.ui.library.AlbumGridScreen
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun LibraryLandingScreen(
    albums: List<AlbumEntity>,
    favoriteAlbumIds: Set<String>,
    albumPlayCounts: Map<String, Long>,
    sortMode: LibrarySortMode,
    viewMode: LibraryViewMode,
    state: LazyGridState,
    flowState: LazyListState,
    effectsEnabled: Boolean,
    onAlbumSelected: (AlbumEntity) -> Unit,
    onFavoriteAlbum: (String) -> Unit,
    onCycleSort: () -> Unit,
    onToggleView: () -> Unit,
) {
    if (albums.isEmpty()) {
        AlbumGridScreen(albums = albums, state = state, onAlbumSelected = onAlbumSelected)
        return
    }

    val orderedAlbums = orderLibraryAlbums(albums, favoriteAlbumIds, sortMode, albumPlayCounts)
    var flowSelectedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = TerminalDimensions.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BracketButton(label = "SORT: ${sortMode.label}", onClick = onCycleSort)
            BracketButton(label = "VIEW: ${viewMode.label}", onClick = onToggleView)
        }
        Box(Modifier.weight(1f)) {
            when (viewMode) {
                LibraryViewMode.GRID -> AlbumGridScreen(
                    albums = orderedAlbums,
                    state = state,
                    onAlbumSelected = onAlbumSelected,
                    favoriteAlbumIds = favoriteAlbumIds,
                    albumPlayCounts = albumPlayCounts,
                    onToggleFavorite = { onFavoriteAlbum(it.id) },
                )
                LibraryViewMode.FLOW -> AlbumCoverFlowScreen(
                    albums = orderedAlbums,
                    state = flowState,
                    favoriteAlbumIds = favoriteAlbumIds,
                    albumPlayCounts = albumPlayCounts,
                    effectsEnabled = effectsEnabled,
                    selectedAlbumId = flowSelectedAlbumId,
                    onSelectedAlbumChanged = { flowSelectedAlbumId = it },
                    onAlbumSelected = onAlbumSelected,
                    onToggleFavorite = { onFavoriteAlbum(it.id) },
                )
            }
        }
    }
}
