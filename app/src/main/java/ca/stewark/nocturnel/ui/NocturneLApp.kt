package ca.stewark.nocturnel.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.playback.PlaybackConnection
import ca.stewark.nocturnel.ui.components.TerminalFrame
import ca.stewark.nocturnel.ui.library.AlbumDetailScreen
import ca.stewark.nocturnel.ui.library.AlbumGridScreen
import ca.stewark.nocturnel.ui.library.LibrarySetupScreen
import ca.stewark.nocturnel.ui.library.LibrarySourceViewModel
import ca.stewark.nocturnel.ui.playlist.PlaylistsScreen

private enum class Destination { LIBRARY, PLAYLISTS, NOW_PLAYING, SETTINGS }

@Composable
fun NocturneLApp(viewModel: LibrarySourceViewModel = viewModel()) {
    val albums by viewModel.albums.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(viewModel::selectFolder) }
    val context = LocalContext.current
    val playback = remember(context) { PlaybackConnection(context) }
    var destination by remember { mutableStateOf(Destination.LIBRARY) }
    var selectedAlbum by remember { mutableStateOf<AlbumEntity?>(null) }

    if (viewModel.source == null) {
        LibrarySetupScreen { launcher.launch(null) }
        return
    }
    selectedAlbum?.let { album ->
        val tracks by viewModel.tracks(album.id).collectAsState(emptyList())
        AlbumDetailScreen(album, tracks, onBack = { selectedAlbum = null }, onPlay = playback::play)
        return
    }

    Scaffold(bottomBar = {
        NavigationBar {
            Destination.entries.forEach { item ->
                NavigationBarItem(selected = destination == item, onClick = { destination = item }, icon = { Text("•") }, label = { Text(item.name.replace('_', ' ')) })
            }
        }
    }) { inset ->
        when (destination) {
            Destination.LIBRARY -> Column(Modifier.fillMaxSize().padding(inset)) {
                TerminalFrame("${viewModel.source?.displayName ?: "MUSIC FOLDER"}") {
                    Button(onClick = viewModel::rescan, enabled = !viewModel.scanState.running) { Text(if (viewModel.scanState.running) "SCANNING ${viewModel.scanState.progress}" else "RESCAN LIBRARY") }
                    viewModel.scanState.message?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
                    viewModel.scanState.report?.let { Text("Added ${it.added} · Missing ${it.missing} · Skipped ${it.skipped}") }
                }
                AlbumGridScreen(albums, onAlbumSelected = { selectedAlbum = it })
            }
            Destination.PLAYLISTS -> PlaylistsScreen()
            Destination.NOW_PLAYING -> NowPlayingScreen(playback)
            Destination.SETTINGS -> SettingsScreen(onChooseFolder = { launcher.launch(null) })
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, message: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) { TerminalFrame(title) { Text(message, modifier = Modifier.padding(top = 8.dp)) } }
}

@Composable
private fun NowPlayingScreen(playback: PlaybackConnection) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TerminalFrame("NOW PLAYING") {
            Text("Playback controls remain available from Android's notification and lock screen.", modifier = Modifier.padding(top = 8.dp))
            Button(onClick = playback::previous, modifier = Modifier.padding(top = 12.dp)) { Text("PREVIOUS") }
            Button(onClick = playback::toggle, modifier = Modifier.padding(top = 8.dp)) { Text("PLAY / PAUSE") }
            Button(onClick = playback::next, modifier = Modifier.padding(top = 8.dp)) { Text("NEXT") }
        }
    }
}

@Composable
private fun SettingsScreen(onChooseFolder: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) { TerminalFrame("SETTINGS") { Button(onClick = onChooseFolder, modifier = Modifier.padding(top = 8.dp)) { Text("CHANGE MUSIC FOLDER") } } }
}
