package ca.stewark.nocturnel.ui.listening

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.library.AlbumGridScreen
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun LibraryLandingScreen(
    albums: List<AlbumEntity>,
    favoriteAlbumIds: Set<String>,
    albumPlayCounts: Map<String, Long>,
    sortMode: LibrarySortMode,
    state: LazyGridState,
    onAlbumSelected: (AlbumEntity) -> Unit,
    onFavoriteAlbum: (String) -> Unit,
    onCycleSort: () -> Unit,
) {
    if (albums.isEmpty()) {
        AlbumGridScreen(albums = albums, state = state, onAlbumSelected = onAlbumSelected)
        return
    }

    Column(Modifier.fillMaxSize()) {
        BracketButton(
            label = "SORT: ${sortMode.label}",
            onClick = onCycleSort,
            modifier = Modifier.padding(horizontal = TerminalDimensions.sm),
        )
        Box(Modifier.weight(1f)) {
            AlbumGridScreen(
                albums = orderLibraryAlbums(albums, favoriteAlbumIds, sortMode, albumPlayCounts),
                state = state,
                onAlbumSelected = onAlbumSelected,
                favoriteAlbumIds = favoriteAlbumIds,
                albumPlayCounts = albumPlayCounts,
                onToggleFavorite = { onFavoriteAlbum(it.id) },
            )
        }
    }
}
