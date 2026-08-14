package ca.stewark.nocturnel.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.playback.PlaybackConnection
import ca.stewark.nocturnel.ui.components.TerminalScaffold
import ca.stewark.nocturnel.ui.library.AlbumDetailScreen
import ca.stewark.nocturnel.ui.library.AlbumGridScreen
import ca.stewark.nocturnel.ui.library.ArtistDetailScreen
import ca.stewark.nocturnel.ui.library.ArtistRow
import ca.stewark.nocturnel.ui.library.ArtistsScreen
import ca.stewark.nocturnel.ui.library.LibrarySetupScreen
import ca.stewark.nocturnel.ui.library.LibrarySourceViewModel
import ca.stewark.nocturnel.ui.library.SearchScreen
import ca.stewark.nocturnel.ui.library.groupArtists
import ca.stewark.nocturnel.ui.navigation.NocturneLDestination
import ca.stewark.nocturnel.ui.playback.NowPlayingScreen
import ca.stewark.nocturnel.ui.playlist.PlaylistsScreen
import ca.stewark.nocturnel.ui.playlist.AlbumPlaylistUiState
import ca.stewark.nocturnel.ui.playlist.PlaylistViewModel
import ca.stewark.nocturnel.ui.settings.SettingsScreen
import ca.stewark.nocturnel.ui.settings.SettingsViewModel

@Composable
fun NocturneLApp(
    viewModel: LibrarySourceViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    playlistViewModel: PlaylistViewModel = viewModel(),
) {
    val albums by viewModel.albums.collectAsState()
    val tracks by viewModel.playableTracks.collectAsState()
    val settings by settingsViewModel.state.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val albumPlaylistState by playlistViewModel.albumPlaylistState.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(viewModel::selectFolder) }
    var artworkAlbum by remember { mutableStateOf<AlbumEntity?>(null) }
    val artworkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        artworkAlbum?.let { viewModel.setManualArtwork(it.id, uri) }
        artworkAlbum = null
    }
    val context = LocalContext.current
    val playback = remember(context) { PlaybackConnection(context) }
    val playbackState by playback.state.collectAsState()
    DisposableEffect(playback) { onDispose(playback::release) }

    var destinationName by rememberSaveable { mutableStateOf(NocturneLDestination.LIBRARY.name) }
    val destination = NocturneLDestination.entries.firstOrNull { it.name == destinationName } ?: NocturneLDestination.LIBRARY
    var selectedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    var playlistPickerExpanded by rememberSaveable(selectedAlbumId) { mutableStateOf(false) }
    var selectedArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedAlbum = albums.firstOrNull { it.id == selectedAlbumId }
    val selectedArtist: ArtistRow? = groupArtists(albums).firstOrNull { it.name == selectedArtistName }
    LaunchedEffect(selectedAlbumId) {
        playlistPickerExpanded = false
        playlistViewModel.clearAlbumPlaylistState()
    }
    LaunchedEffect(albumPlaylistState) {
        if (albumPlaylistState is AlbumPlaylistUiState.Success) playlistPickerExpanded = false
    }
    BackHandler(enabled = playlistPickerExpanded || selectedAlbumId != null || selectedArtistName != null) {
        when {
            playlistPickerExpanded -> playlistPickerExpanded = false
            selectedAlbumId != null -> selectedAlbumId = null
            else -> selectedArtistName = null
        }
    }

    if (viewModel.source == null) {
        LibrarySetupScreen { launcher.launch(null) }
        return
    }

    TerminalScaffold(
        selected = destination,
        onSelected = {
            destinationName = it.name
            selectedAlbumId = null
            selectedArtistName = null
        },
        effectsEnabled = settings.effectiveEffectsEnabled,
        status = viewModel.scanState.message,
    ) {
        when {
            selectedAlbum != null -> {
                val albumTracks by viewModel.tracks(selectedAlbum.id).collectAsState(emptyList())
                AlbumDetailScreen(
                    selectedAlbum,
                    albumTracks,
                    onBack = {
                        playlistViewModel.clearAlbumPlaylistState()
                        selectedAlbumId = null
                    },
                    onPlay = playback::play,
                    onPlayAlbum = playback::playQueue,
                    onChooseArtwork = {
                        artworkAlbum = selectedAlbum
                        artworkLauncher.launch(arrayOf("image/*"))
                    },
                    onClearArtwork = { viewModel.setManualArtwork(selectedAlbum.id, null) },
                    playlists = playlists,
                    playlistPickerExpanded = playlistPickerExpanded,
                    albumPlaylistState = albumPlaylistState,
                    onTogglePlaylistPicker = {
                        playlistViewModel.clearAlbumPlaylistState()
                        playlistPickerExpanded = !playlistPickerExpanded
                    },
                    onAddAlbumToPlaylist = {
                        playlistViewModel.addAlbum(it.id, it.name, albumTracks)
                    },
                    onCreatePlaylistAndAdd = {
                        playlistViewModel.createAndAddAlbum(it, albumTracks)
                    },
                )
            }
            selectedArtist != null -> ArtistDetailScreen(
                selectedArtist,
                onBack = { selectedArtistName = null },
                onAlbumSelected = { selectedAlbumId = it.id },
            )
            else -> when (destination) {
                NocturneLDestination.LIBRARY -> LibraryScreen(albums) { selectedAlbumId = it.id }
                NocturneLDestination.SEARCH -> SearchScreen(
                    tracks,
                    albums,
                    playback::play,
                    onAlbumSelected = { selectedAlbumId = it.id },
                    onArtistSelected = { selectedArtistName = it.name },
                )
                NocturneLDestination.ARTISTS -> ArtistsScreen(albums) { selectedArtistName = it.name }
                NocturneLDestination.PLAYLISTS -> PlaylistsScreen(viewModel = playlistViewModel, playback = playback)
                NocturneLDestination.NOW_PLAYING -> NowPlayingScreen(
                    state = playbackState,
                    albumArtwork = albums.firstOrNull { it.id == playbackState.albumId },
                    effectsEnabled = settings.effectiveEffectsEnabled,
                    onPrevious = playback::previous,
                    onToggle = playback::toggle,
                    onNext = playback::next,
                    onShuffle = playback::toggleShuffle,
                    onRepeat = playback::cycleRepeat,
                    onSeek = playback::seekTo,
                )
                NocturneLDestination.SETTINGS -> SettingsScreen(
                    onChooseFolder = { launcher.launch(null) },
                    onRescan = viewModel::rescan,
                    state = settings,
                    onEffectsChanged = settingsViewModel::setEffectsEnabled,
                    scanRunning = viewModel.scanState.running,
                )
            }
        }
    }
}

@Composable
internal fun LibraryScreen(
    albums: List<AlbumEntity>,
    onAlbumSelected: (AlbumEntity) -> Unit,
) {
    AlbumGridScreen(albums, onAlbumSelected)
}
