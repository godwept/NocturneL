package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun ArtistsScreen(albums: List<AlbumEntity>, onArtistSelected: (ArtistRow) -> Unit) {
    val artists = groupArtists(albums)
    Column(Modifier.fillMaxSize().padding(TerminalDimensions.md)) {
        AsciiFrame("ARTISTS") { Text("${artists.size} LOCAL ARTIST(S)") }
        LazyColumn {
            items(artists, key = { it.name.lowercase() }) { artist ->
                Row(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = TerminalDimensions.minimumTouchTarget)
                        .clickable { onArtistSelected(artist) }.padding(vertical = TerminalDimensions.sm),
                ) {
                    Text("[ ${artist.name} ]", Modifier.weight(1f))
                    Text("${artist.albums.size} ALBUM(S)", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
