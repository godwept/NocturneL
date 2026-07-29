package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.ui.artwork.RetroArtwork
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun AlbumGridScreen(albums: List<AlbumEntity>, onAlbumSelected: (AlbumEntity) -> Unit) {
    if (albums.isEmpty()) {
        TerminalNotice("No playable albums yet. Rescan after adding music.", Modifier.padding(TerminalDimensions.lg))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TerminalDimensions.sm),
        horizontalArrangement = Arrangement.spacedBy(TerminalDimensions.sm),
        verticalArrangement = Arrangement.spacedBy(TerminalDimensions.sm),
    ) {
        items(albums, key = { it.id }) { album ->
            AsciiFrame(
                modifier = Modifier.clickable { onAlbumSelected(album) },
            ) {
                RetroArtwork(album, Modifier.fillMaxWidth().aspectRatio(1f))
                Text(album.title.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = TerminalDimensions.xs))
                Text(album.artist, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
