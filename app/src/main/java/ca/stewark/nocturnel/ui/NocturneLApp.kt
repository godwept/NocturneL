package ca.stewark.nocturnel.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import ca.stewark.nocturnel.ui.playback.QueueEditorScreen
import ca.stewark.nocturnel.ui.playback.toQueueEditorState
import ca.stewark.nocturnel.ui.playlist.PlaylistsScreen
import ca.stewark.nocturnel.ui.playlist.AlbumPlaylistUiState
import ca.stewark.nocturnel.ui.playlist.PlaylistViewModel
import ca.stewark.nocturnel.ui.settings.SettingsScreen
import ca.stewark.nocturnel.ui.settings.SettingsViewModel
import ca.stewark.nocturnel.NocturneLApplication
import ca.stewark.nocturnel.ui.listening.FavoritesScreen
import ca.stewark.nocturnel.ui.listening.LibraryLandingScreen
import ca.stewark.nocturnel.ui.listening.ListeningHistoryScreen
import ca.stewark.nocturnel.ui.listening.ListeningViewModel
import ca.stewark.nocturnel.ui.listening.resumeState

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
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(viewModel::requestFolder) }
    var artworkAlbum by remember { mutableStateOf<AlbumEntity?>(null) }
    val artworkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        artworkAlbum?.let { viewModel.setManualArtwork(it.id, uri) }
        artworkAlbum = null
    }
    val context = LocalContext.current
    val application = context.applicationContext as NocturneLApplication
    val listeningViewModel: ListeningViewModel = viewModel(factory = remember(application) { ListeningViewModel.Factory(application) })
    val listening by listeningViewModel.state.collectAsState()
    val playback = remember(context) { PlaybackConnection(context) }
    val playbackState by playback.state.collectAsState()
    val analysisFrame by playback.analysisState.collectAsState()
    DisposableEffect(playback) { onDispose(playback::release) }
    LaunchedEffect(settings.visualizerSyncOffsetMs) {
        playback.setVisualizerSyncOffsetMs(settings.visualizerSyncOffsetMs)
    }

    var destinationName by rememberSaveable { mutableStateOf(NocturneLDestination.LIBRARY.name) }
    val destination = NocturneLDestination.entries.firstOrNull { it.name == destinationName } ?: NocturneLDestination.LIBRARY
    var selectedAlbumId by rememberSaveable { mutableStateOf<String?>(null) }
    val libraryGridState = rememberLazyGridState()
    var playlistPickerExpanded by rememberSaveable(selectedAlbumId) { mutableStateOf(false) }
    var selectedArtistName by rememberSaveable { mutableStateOf<String?>(null) }
    var queueEditorOpen by rememberSaveable { mutableStateOf(false) }
    var librarySubview by rememberSaveable { mutableStateOf("LANDING") }
    val selectedAlbum = albums.firstOrNull { it.id == selectedAlbumId }
    val selectedArtist: ArtistRow? = groupArtists(albums).firstOrNull { it.name == selectedArtistName }
    LaunchedEffect(selectedAlbumId) {
        playlistPickerExpanded = false
        playlistViewModel.clearAlbumPlaylistState()
    }
    LaunchedEffect(albumPlaylistState) {
        if (albumPlaylistState is AlbumPlaylistUiState.Success) playlistPickerExpanded = false
    }
    BackHandler(enabled = queueEditorOpen || playlistPickerExpanded || selectedAlbumId != null || selectedArtistName != null || librarySubview != "LANDING") {
        when {
            queueEditorOpen -> { playback.expireQueueUndo(); queueEditorOpen = false }
            playlistPickerExpanded -> playlistPickerExpanded = false
            selectedAlbumId != null -> selectedAlbumId = null
            selectedArtistName != null -> selectedArtistName = null
            else -> librarySubview = "LANDING"
        }
    }

    if (viewModel.source == null) {
        LibrarySetupScreen { launcher.launch(null) }
        return
    }

    TerminalScaffold(
        selected = destination,
        onSelected = {
            if (queueEditorOpen) playback.expireQueueUndo()
            queueEditorOpen = false
            destinationName = it.name
            selectedAlbumId = null
            selectedArtistName = null
            librarySubview = "LANDING"
        },
        effectsEnabled = settings.effectiveEffectsEnabled,
        status = if (queueEditorOpen) viewModel.scanState.message else playbackState.queueNotice ?: listening.message ?: viewModel.scanState.message,
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
                    onAddAlbumToQueue = playback::addToQueue,
                    onAddTrackToQueue = { playback.addToQueue(listOf(it)) },
                    onChooseArtwork = {
                        artworkAlbum = selectedAlbum
                        artworkLauncher.launch(arrayOf("image/*"))
                    },
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
                    albumFavorite = selectedAlbum.id in listening.favoriteAlbumIds,
                    albumPlayCount = listening.albumPlayCounts[selectedAlbum.id] ?: 0,
                    favoriteTrackPaths = listening.favoriteTrackPaths,
                    trackPlayCounts = listening.trackPlayCounts,
                    onToggleAlbumFavorite = { listeningViewModel.toggleAlbum(it.id) },
                    onToggleTrackFavorite = { listeningViewModel.toggleTrack(it.relativePath) },
                )
            }
            selectedArtist != null -> ArtistDetailScreen(
                selectedArtist,
                onBack = { selectedArtistName = null },
                onAlbumSelected = { selectedAlbumId = it.id },
                favoriteAlbumIds = listening.favoriteAlbumIds,
                albumPlayCounts = listening.albumPlayCounts,
                onToggleFavorite = { listeningViewModel.toggleAlbum(it.id) },
            )
            else -> when (destination) {
                NocturneLDestination.LIBRARY -> when (librarySubview) {
                    "FAVORITES" -> FavoritesScreen(
                        listening,
                        onBack = { librarySubview = "LANDING" },
                        onAlbumSelected = { selectedAlbumId = it.id },
                        onTrackSelected = playback::play,
                        onFavoriteAlbum = listeningViewModel::toggleAlbum,
                        onFavoriteTrack = listeningViewModel::toggleTrack,
                    )
                    "HISTORY" -> ListeningHistoryScreen(
                        listening,
                        onBack = { librarySubview = "LANDING" },
                        onTrackSelected = playback::play,
                        onFavoriteTrack = listeningViewModel::toggleTrack,
                    )
                    else -> LibraryLandingScreen(
                        albums = albums,
                        listening = listening,
                        resume = resumeState(playbackState, viewModel.source?.accessLost != true),
                        state = libraryGridState,
                        onResume = playback::toggle,
                        onAlbumSelected = { selectedAlbumId = it.id },
                        onTrackSelected = playback::play,
                        onFavoriteAlbum = listeningViewModel::toggleAlbum,
                        onFavoriteTrack = listeningViewModel::toggleTrack,
                        onViewFavorites = { librarySubview = "FAVORITES" },
                        onViewHistory = { librarySubview = "HISTORY" },
                    )
                }
                NocturneLDestination.SEARCH -> SearchScreen(
                    tracks,
                    albums,
                    playback::play,
                    onAlbumSelected = { selectedAlbumId = it.id },
                    onArtistSelected = { selectedArtistName = it.name },
                    onAddToQueue = { playback.addToQueue(listOf(it)) },
                    favoriteAlbumIds = listening.favoriteAlbumIds,
                    favoriteTrackPaths = listening.favoriteTrackPaths,
                    albumPlayCounts = listening.albumPlayCounts,
                    trackPlayCounts = listening.trackPlayCounts,
                    onToggleAlbumFavorite = { listeningViewModel.toggleAlbum(it.id) },
                    onToggleTrackFavorite = { listeningViewModel.toggleTrack(it.relativePath) },
                )
                NocturneLDestination.ARTISTS -> ArtistsScreen(albums) { selectedArtistName = it.name }
                NocturneLDestination.PLAYLISTS -> PlaylistsScreen(
                    viewModel = playlistViewModel,
                    playback = playback,
                )
                NocturneLDestination.NOW_PLAYING -> if (queueEditorOpen) {
                    QueueEditorScreen(
                        state = playbackState.toQueueEditorState(),
                        onBack = { queueEditorOpen = false },
                        onJump = playback::jumpToQueueOccurrence,
                        onMove = playback::moveQueueOccurrence,
                        onRemove = playback::removeQueueOccurrence,
                        onUndo = playback::undoQueueRemoval,
                        onClear = playback::clearUpcomingQueue,
                        onExpireUndo = playback::expireQueueUndo,
                    )
                } else {
                    NowPlayingScreen(
                        state = playbackState,
                        albumArtwork = albums.firstOrNull { it.id == playbackState.albumId },
                        effectsEnabled = settings.effectiveEffectsEnabled,
                        onPrevious = playback::previous,
                        onToggle = playback::toggle,
                        onNext = playback::next,
                        onShuffle = playback::toggleShuffle,
                        onRepeat = playback::cycleRepeat,
                        onSeek = playback::seekTo,
                        onOpenQueue = { queueEditorOpen = true },
                        analysisFrame = analysisFrame,
                        onVisualizerActiveChanged = playback::setVisualizerActive,
                        visualizerSyncOffsetMs = settings.visualizerSyncOffsetMs,
                        onDecreaseVisualizerSyncOffset = settingsViewModel::decreaseVisualizerSyncOffset,
                        onIncreaseVisualizerSyncOffset = settingsViewModel::increaseVisualizerSyncOffset,
                        onResetVisualizerSyncOffset = settingsViewModel::resetVisualizerSyncOffset,
                        currentTrackFavorite = playbackState.currentPath in listening.favoriteTrackPaths,
                        currentTrackPlayCount = playbackState.currentPath?.let { listening.trackPlayCounts[it] } ?: 0,
                        onToggleCurrentFavorite = { playbackState.currentPath?.let(listeningViewModel::toggleTrack) },
                    )
                }
                NocturneLDestination.SETTINGS -> SettingsScreen(
                    onChooseFolder = { launcher.launch(null) },
                    onRescan = viewModel::rescan,
                    state = settings,
                    onEffectsChanged = settingsViewModel::setEffectsEnabled,
                    scanRunning = viewModel.scanState.running,
                    onClearListeningData = listeningViewModel::clearHistoryAndCounts,
                    listeningMessage = listening.message,
                    pendingSourceName = viewModel.pendingSourceChange?.displayName,
                    onConfirmSourceChange = viewModel::confirmSourceChange,
                    onCancelSourceChange = viewModel::cancelSourceChange,
                )
            }
        }
    }
}

@Composable
internal fun LibraryScreen(
    albums: List<AlbumEntity>,
    state: LazyGridState = rememberLazyGridState(),
    onAlbumSelected: (AlbumEntity) -> Unit,
) {
    AlbumGridScreen(albums, state, onAlbumSelected)
}
