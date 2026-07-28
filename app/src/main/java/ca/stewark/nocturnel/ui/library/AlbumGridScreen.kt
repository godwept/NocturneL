package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.artwork.TerminalArtworkPlaceholder
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.ui.components.AlbumArtwork
import ca.stewark.nocturnel.ui.components.TerminalFrame

@Composable
fun AlbumGridScreen(albums: List<AlbumEntity>, onAlbumSelected: (AlbumEntity) -> Unit) {
    if (albums.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp)) { Text("No playable albums yet. Select RESCAN after adding music.") }
        return
    }
    LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(albums, key = { it.id }) { album ->
            TerminalFrame("ALBUM", Modifier.clickable { onAlbumSelected(album) }) {
                AlbumArtwork(album, Modifier.fillMaxWidth().padding(top = 10.dp).aspectRatio(1f))
                Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                Text(album.artist, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
