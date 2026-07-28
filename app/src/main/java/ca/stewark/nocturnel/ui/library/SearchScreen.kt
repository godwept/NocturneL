package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.ui.components.TerminalFrame

@Composable
fun SearchScreen(tracks: List<TrackEntity>, onPlay: (TrackEntity) -> Unit) {
    var query by remember { mutableStateOf("") }
    val result = tracks.filter { "${it.title} ${it.artist} ${it.album}".contains(query, ignoreCase = true) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TerminalFrame("SEARCH") {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("SEARCH LOCAL LIBRARY") }, singleLine = true)
        }
        LazyColumn(Modifier.padding(top = 10.dp)) { items(result, key = { it.relativePath }) { track ->
            androidx.compose.material3.TextButton(onClick = { onPlay(track) }) { Text("${track.artist} :: ${track.title}") }
        } }
    }
}
