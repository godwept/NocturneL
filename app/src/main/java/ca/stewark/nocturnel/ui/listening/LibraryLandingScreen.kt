package ca.stewark.nocturnel.ui.listening

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.ui.library.AlbumGridScreen

@Composable
fun LibraryLandingScreen(
    albums: List<AlbumEntity>,
    favoriteAlbumIds: Set<String>,
    albumPlayCounts: Map<String, Long>,
    state: LazyGridState,
    onAlbumSelected: (AlbumEntity) -> Unit,
    onFavoriteAlbum: (String) -> Unit,
) {
    AlbumGridScreen(
        albums = orderLibraryAlbums(albums, favoriteAlbumIds),
        state = state,
        onAlbumSelected = onAlbumSelected,
        favoriteAlbumIds = favoriteAlbumIds,
        albumPlayCounts = albumPlayCounts,
        onToggleFavorite = { onFavoriteAlbum(it.id) },
    )
}
