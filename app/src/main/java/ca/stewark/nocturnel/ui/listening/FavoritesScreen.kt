package ca.stewark.nocturnel.ui.listening

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.library.AlbumGridCard
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun FavoritesScreen(
    state: ListeningUiState,
    onBack: () -> Unit,
    onAlbumSelected: (AlbumEntity) -> Unit,
    onTrackSelected: (TrackEntity) -> Unit,
    onFavoriteAlbum: (String) -> Unit,
    onFavoriteTrack: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(TerminalDimensions.sm)) {
        Row { BracketButton("BACK", onBack) }
        if (state.favoriteAlbums.isEmpty() && state.favoriteTracks.isEmpty()) {
            TerminalNotice("NO FAVORITES YET", Modifier.padding(top = TerminalDimensions.md))
            return@Column
        }
        LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(top = TerminalDimensions.sm)) {
            if (state.favoriteAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { Text("+--[ FAVORITE ALBUMS ]", color = MaterialTheme.colorScheme.secondary) }
                items(state.favoriteAlbums, key = { "album:${it.id}" }) { album ->
                    AlbumGridCard(album, true, state.albumPlayCounts[album.id] ?: 0, onAlbumSelected, { onFavoriteAlbum(it.id) }, Modifier.padding(TerminalDimensions.xs))
                }
            }
            if (state.favoriteTracks.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { Text("+--[ FAVORITE TRACKS ]", color = MaterialTheme.colorScheme.secondary) }
                items(state.favoriteTracks, key = { "track:${it.relativePath}" }, span = { GridItemSpan(maxLineSpan) }) { track ->
                    ListeningTrackRow(track.title, track.artist, state.trackPlayCounts[track.relativePath] ?: 0, true, true, { onTrackSelected(track) }, { onFavoriteTrack(track.relativePath) })
                }
            }
        }
    }
}
