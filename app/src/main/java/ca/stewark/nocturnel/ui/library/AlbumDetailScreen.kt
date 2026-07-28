package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.ui.components.TerminalFrame

@Composable
fun AlbumDetailScreen(album: AlbumEntity, tracks: List<TrackEntity>, onBack: () -> Unit, onPlay: (TrackEntity) -> Unit, onPlayAlbum: (List<TrackEntity>) -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Button(onClick = onBack) { Text("BACK") }
        TerminalFrame(album.title, Modifier.padding(top = 12.dp)) { Text(album.artist) }
        Button(onClick = { onPlayAlbum(tracks) }, modifier = Modifier.padding(top = 8.dp)) { Text("[PLAY ALBUM]") }
        LazyColumn(Modifier.padding(top = 12.dp)) {
            items(tracks, key = { it.relativePath }) { track ->
                Button(onClick = { onPlay(track) }, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("${track.trackNumber?.toString()?.padStart(2, '0') ?: "--"}  ${track.title}")
                }
            }
        }
    }
}
