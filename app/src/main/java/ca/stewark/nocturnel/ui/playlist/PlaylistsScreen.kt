package ca.stewark.nocturnel.ui.playlist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.playback.PlaybackConnection
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.components.TerminalTextField
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import kotlinx.coroutines.launch

@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel = viewModel(),
    playback: PlaybackConnection? = null,
    favoriteTrackPaths: Set<String> = emptySet(),
    trackPlayCounts: Map<String, Long> = emptyMap(),
    onToggleTrackFavorite: (ca.stewark.nocturnel.data.entity.TrackEntity) -> Unit = {},
) {
    val playlists by viewModel.playlists.collectAsState()
    val detail by viewModel.detail.collectAsState()
    var newName by remember { mutableStateOf("") }
    var exportPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    val context = LocalContext.current
    val player = playback ?: remember(context) { PlaybackConnection(context) }
    val scope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) viewModel.importCancelled() else viewModel.import(uri)
    }
    val exportAllLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) viewModel.exportCancelled() else viewModel.exportAll(uri)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        exportPlaylist?.let { playlist ->
            if (uri == null) viewModel.exportCancelled() else viewModel.export(playlist.id, uri)
        }
        exportPlaylist = null
    }
    detail?.let { state ->
        BackHandler(onBack = viewModel::close)
        PlaylistDetailScreen(
            state,
            viewModel::close,
            { scope.launch { player.playQueue(viewModel.playableTracks(state.playlist.id)) } },
            { viewModel.rename(state.playlist.id, it) },
            { viewModel.add(state.playlist.id, it) },
            { viewModel.remove(state.playlist.id, it) },
            { from, to -> viewModel.move(state.playlist.id, from, to) },
            { tracks, skipped -> player.playNext(tracks, skipped) },
            { tracks, skipped -> player.addToQueue(tracks, skipped) },
            { player.playNext(listOf(it)) },
            { player.addToQueue(listOf(it)) },
            favoriteTrackPaths,
            trackPlayCounts,
            onToggleTrackFavorite,
        )
        return
    }
    PlaylistIndexScreen(
        playlists = playlists,
        message = viewModel.message,
        newName = newName,
        onNewNameChange = { newName = it },
        onCreate = { viewModel.create(newName); newName = "" },
        onImport = {
            importLauncher.launch(arrayOf(
                "audio/x-mpegurl",
                "application/vnd.apple.mpegurl",
                "text/plain",
                "application/zip",
                "application/x-zip-compressed",
            ))
        },
        onExportAll = { exportAllLauncher.launch("NocturneL Playlists.zip") },
        onOpen = viewModel::open,
        onPlay = { playlist -> scope.launch { player.playQueue(viewModel.playableTracks(playlist.id)) } },
        onExport = { playlist -> exportPlaylist = playlist; exportLauncher.launch("${playlist.name}.m3u8") },
        onDelete = viewModel::delete,
    )
}

@Composable
internal fun PlaylistIndexScreen(
    playlists: List<PlaylistEntity>,
    message: String?,
    newName: String,
    onNewNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onImport: () -> Unit,
    onExportAll: () -> Unit,
    onOpen: (Long) -> Unit,
    onPlay: (PlaylistEntity) -> Unit,
    onExport: (PlaylistEntity) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(TerminalDimensions.md)) {
        AsciiFrame("PLAYLISTS") {
            TerminalTextField(newName, onNewNameChange, "NEW PLAYLIST")
            Row {
                BracketButton("CREATE", onCreate, enabled = newName.isNotBlank())
                BracketButton("IMPORT", onImport)
                BracketButton("EXPORT ALL", onExportAll)
            }
            message?.let { TerminalNotice(it) }
        }
        LazyColumn(Modifier.padding(top = TerminalDimensions.sm)) {
            items(playlists, key = { it.id }) { playlist ->
                AsciiFrame(playlist.name, Modifier.padding(vertical = TerminalDimensions.xxs).clickable { onOpen(playlist.id) }) {
                    Row(Modifier.fillMaxWidth()) {
                        BracketButton("OPEN", { onOpen(playlist.id) })
                        BracketButton("PLAY", { onPlay(playlist) })
                        BracketButton("EXPORT", { onExport(playlist) })
                        BracketButton("DELETE", { onDelete(playlist.id) })
                    }
                    Text("Select OPEN to rename or edit tracks.")
                }
            }
        }
    }
}
