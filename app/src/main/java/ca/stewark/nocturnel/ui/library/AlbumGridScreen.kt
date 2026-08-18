package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.ui.artwork.RetroArtwork
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.components.FavoriteToggle
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun AlbumGridScreen(
    albums: List<AlbumEntity>,
    state: LazyGridState = rememberLazyGridState(),
    onAlbumSelected: (AlbumEntity) -> Unit,
    favoriteAlbumIds: Set<String> = emptySet(),
    albumPlayCounts: Map<String, Long> = emptyMap(),
    onToggleFavorite: (AlbumEntity) -> Unit = {},
) {
    if (albums.isEmpty()) {
        TerminalNotice("No playable albums yet. Rescan after adding music.", Modifier.padding(TerminalDimensions.lg))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TerminalDimensions.sm),
        horizontalArrangement = Arrangement.spacedBy(TerminalDimensions.sm),
        verticalArrangement = Arrangement.spacedBy(TerminalDimensions.sm),
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumGridCard(album, album.id in favoriteAlbumIds, albumPlayCounts[album.id] ?: 0, onAlbumSelected, onToggleFavorite)
        }
    }
}

@Composable
internal fun AlbumGridCard(
    album: AlbumEntity,
    favorite: Boolean,
    playCount: Long,
    onAlbumSelected: (AlbumEntity) -> Unit,
    onToggleFavorite: (AlbumEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    AsciiFrame(modifier = modifier.clickable { onAlbumSelected(album) }) {
        RetroArtwork(album, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(album.title.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = TerminalDimensions.xs))
        Text(album.artist, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$playCount×", color = MaterialTheme.colorScheme.secondary)
            FavoriteToggle(album.title, favorite, { onToggleFavorite(album) })
        }
    }
}
