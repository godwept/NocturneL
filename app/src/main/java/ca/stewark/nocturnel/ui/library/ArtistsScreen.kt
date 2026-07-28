package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.ui.components.TerminalFrame

@Composable
fun ArtistsScreen(tracks: List<TrackEntity>) {
    val artists = tracks.groupBy { it.artist }.mapValues { (_, values) -> values.map { it.album }.distinct().size }.toList().sortedBy { it.first }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TerminalFrame("ARTISTS") { Text("${artists.size} LOCAL ARTIST(S)") }
        LazyColumn { items(artists, key = { it.first }) { (artist, albums) -> Text("[ $artist ]  $albums ALBUM(S)", modifier = Modifier.padding(vertical = 10.dp)) } }
    }
}
