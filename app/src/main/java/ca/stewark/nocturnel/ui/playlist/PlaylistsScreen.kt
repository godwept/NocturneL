package ca.stewark.nocturnel.ui.playlist

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.ui.components.TerminalFrame

@Composable
fun PlaylistsScreen(viewModel: PlaylistViewModel = viewModel()) {
    val playlists by viewModel.playlists.collectAsState()
    var newName by remember { mutableStateOf("") }
    var exportPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::import) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        exportPlaylist?.let { playlist -> uri?.let { viewModel.export(playlist.id, it) } }
        exportPlaylist = null
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TerminalFrame("PLAYLISTS") {
            OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("NEW PLAYLIST") }, singleLine = true)
            Button(onClick = { viewModel.create(newName); newName = "" }, modifier = Modifier.padding(top = 8.dp)) { Text("[CREATE]") }
            Button(onClick = { importLauncher.launch(arrayOf("audio/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain")) }, modifier = Modifier.padding(top = 8.dp)) { Text("IMPORT M3U8") }
            viewModel.message?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
        LazyColumn(Modifier.padding(top = 12.dp)) {
            items(playlists, key = { it.id }) { playlist ->
                TerminalFrame(playlist.name, Modifier.padding(vertical = 4.dp)) {
                    Button(onClick = { viewModel.rename(playlist.id, "${playlist.name} (edited)") }, modifier = Modifier.padding(top = 8.dp)) { Text("[RENAME]") }
                    Button(onClick = { viewModel.delete(playlist.id) }, modifier = Modifier.padding(top = 8.dp)) { Text("[DELETE]") }
                    Button(onClick = { exportPlaylist = playlist; exportLauncher.launch("${playlist.name}.m3u8") }, modifier = Modifier.padding(top = 8.dp)) { Text("EXPORT M3U8") }
                }
            }
        }
    }
}
