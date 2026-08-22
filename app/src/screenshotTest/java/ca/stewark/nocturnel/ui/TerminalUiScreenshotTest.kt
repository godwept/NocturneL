package ca.stewark.nocturnel.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.data.model.PlaylistEntryRow
import ca.stewark.nocturnel.playback.PlaybackQueueItem
import ca.stewark.nocturnel.playback.PlaybackUiState
import ca.stewark.nocturnel.playback.QueueEntry
import ca.stewark.nocturnel.ui.library.AlbumDetailScreen
import ca.stewark.nocturnel.ui.library.AlbumGridScreen
import ca.stewark.nocturnel.ui.library.ArtistsScreen
import ca.stewark.nocturnel.ui.library.LibrarySetupScreen
import ca.stewark.nocturnel.ui.library.SearchScreen
import ca.stewark.nocturnel.ui.listening.LibraryLandingScreen
import ca.stewark.nocturnel.ui.listening.LibrarySortMode
import ca.stewark.nocturnel.ui.listening.LibraryViewMode
import ca.stewark.nocturnel.ui.playback.NowPlayingScreen
import ca.stewark.nocturnel.ui.playback.QueueEditorScreen
import ca.stewark.nocturnel.ui.playback.QueueEditorRow
import ca.stewark.nocturnel.ui.playback.QueueEditorTrack
import ca.stewark.nocturnel.ui.playback.UpcomingQueueRow
import ca.stewark.nocturnel.ui.playback.queueEditorState
import ca.stewark.nocturnel.ui.playback.visualizer.TerminalVisualizerScene
import ca.stewark.nocturnel.ui.playback.visualizer.VisualizerDisplayMode
import ca.stewark.nocturnel.ui.playback.visualizer.VisualizerSyncControls
import ca.stewark.nocturnel.ui.playlist.PlaylistDetailScreen
import ca.stewark.nocturnel.ui.playlist.PlaylistTrackEntryRow
import ca.stewark.nocturnel.ui.playlist.PlaylistTrackRow
import ca.stewark.nocturnel.ui.playlist.playlistDetailState
import ca.stewark.nocturnel.ui.settings.SettingsScreen
import ca.stewark.nocturnel.ui.settings.TerminalSettingsState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import ca.stewark.nocturnel.ui.components.TerminalScaffold
import ca.stewark.nocturnel.ui.navigation.NocturneLDestination
import com.android.tools.screenshot.PreviewTest
import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame

private val previewAlbums = listOf(
    AlbumEntity("red", "red", "Red Horizon", "Signal One", "2026", null, null, null),
    AlbumEntity("blue", "blue", "Blue Static", "Signal Two", "2025", null, null, null),
    AlbumEntity("amber", "amber", "Amber Relay", "Signal Three", "2024", null, null, null),
)
private val previewTracks = previewAlbums.flatMapIndexed { index, album ->
    listOf(
        TrackEntity("${album.id}/01.flac", "content://${album.id}/1", album.id, "Carrier ${index + 1}", album.artist, album.title, 183_000, 1, 1, "PLAYABLE", 1),
        TrackEntity("${album.id}/02.flac", "content://${album.id}/2", album.id, "Afterimage ${index + 1}", album.artist, album.title, 241_000, 2, 1, "PLAYABLE", 1),
    )
}
private val radarFrame = AudioAnalysisFrame(
    waveform = List(128) { 0f },
    bands = List(32) { index -> if (index < 10) .85f - index * .04f else .12f },
    energy = .72f,
    lowEnergy = .88f,
    midEnergy = .45f,
    highEnergy = .18f,
    transient = .9f,
    frameId = 41,
    status = AnalysisStatus.ACTIVE,
)
private val spectrumFrame = radarFrame.copy(
    bands = List(32) { index -> ((index % 8) + 1) / 8f },
    transient = .35f,
    frameId = 42,
)

@Composable
private fun TerminalPreview(content: @Composable () -> Unit) = NocturneLTheme {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
}

@PreviewTest
@Preview(name = "Album grid", widthDp = 412, heightDp = 915)
@Composable
fun AlbumGridPreview() = TerminalPreview { AlbumGridScreen(previewAlbums, onAlbumSelected = {}) }

@PreviewTest
@Preview(name = "Root effects off", widthDp = 412, heightDp = 915)
@Composable
fun RootPreview() = TerminalPreview {
    TerminalScaffold(NocturneLDestination.LIBRARY, {}, effectsEnabled = false, status = "LIBRARY READY") {
        LibraryLandingScreen(
            albums = previewAlbums,
            favoriteAlbumIds = setOf("red"),
            albumPlayCounts = previewAlbums.associate { it.id to 12L },
            sortMode = LibrarySortMode.ARTIST,
            viewMode = LibraryViewMode.GRID,
            state = rememberLazyGridState(),
            flowState = rememberLazyListState(),
            effectsEnabled = false,
            onAlbumSelected = {},
            onFavoriteAlbum = {},
            onCycleSort = {},
            onToggleView = {},
        )
    }
}

@PreviewTest
@Preview(name = "Cover flow", widthDp = 412, heightDp = 915)
@Composable
fun CoverFlowPreview() = coverFlowPreview(effectsEnabled = true)

@PreviewTest
@Preview(name = "Cover flow effects off", widthDp = 412, heightDp = 915)
@Composable
fun CoverFlowEffectsOffPreview() = coverFlowPreview(effectsEnabled = false)

@Composable
private fun coverFlowPreview(effectsEnabled: Boolean) = TerminalPreview {
    TerminalScaffold(NocturneLDestination.LIBRARY, {}, effectsEnabled = effectsEnabled, status = "LIBRARY READY") {
        LibraryLandingScreen(
            albums = previewAlbums,
            favoriteAlbumIds = setOf("red"),
            albumPlayCounts = previewAlbums.associate { it.id to 12L },
            sortMode = LibrarySortMode.ARTIST,
            viewMode = LibraryViewMode.FLOW,
            state = rememberLazyGridState(),
            flowState = rememberLazyListState(initialFirstVisibleItemIndex = 1),
            effectsEnabled = effectsEnabled,
            onAlbumSelected = {},
            onFavoriteAlbum = {},
            onCycleSort = {},
            onToggleView = {},
        )
    }
}

@PreviewTest
@Preview(name = "Settings source confirmation", widthDp = 412, heightDp = 915)
@Composable
fun SettingsSourceConfirmationPreview() = TerminalPreview {
    SettingsScreen({}, {}, TerminalSettingsState(), {}, pendingSourceName = "ARCHIVE")
}

@PreviewTest
@Preview(name = "Setup", widthDp = 412, heightDp = 915)
@Composable
fun SetupPreview() = TerminalPreview { LibrarySetupScreen {} }

@PreviewTest
@Preview(name = "Album detail", widthDp = 412, heightDp = 915)
@Composable
fun AlbumDetailPreview() = TerminalPreview {
    val tracks = previewTracks.take(2).mapIndexed { index, track -> if (index == 0) track.copy(title = previewLongTrackTitle) else track }
    AlbumDetailScreen(
        album = previewAlbums.first(),
        tracks = tracks,
        onBack = {},
        onPlay = {},
        onPlayAlbum = {},
        onChooseArtwork = {},
        playlists = listOf(
            PlaylistEntity(1, "Night Run", 1),
            PlaylistEntity(2, "Deep Focus", 2),
        ),
        playlistPickerExpanded = true,
    )
}

@PreviewTest
@Preview(name = "Album detail empty playlist", widthDp = 412, heightDp = 915)
@Composable
fun AlbumDetailEmptyPlaylistPreview() = TerminalPreview {
    val tracks = previewTracks.take(2).mapIndexed { index, track -> if (index == 0) track.copy(title = previewLongTrackTitle) else track }
    AlbumDetailScreen(
        album = previewAlbums.first(),
        tracks = tracks,
        onBack = {},
        onPlay = {},
        onPlayAlbum = {},
        onChooseArtwork = {},
        playlistPickerExpanded = true,
    )
}

@PreviewTest
@Preview(name = "Artists", widthDp = 412, heightDp = 915)
@Composable
fun ArtistsPreview() = TerminalPreview { ArtistsScreen(previewAlbums) {} }

@PreviewTest
@Preview(name = "Search", widthDp = 412, heightDp = 915)
@Composable
fun SearchPreview() = TerminalPreview {
    val tracks = previewTracks.mapIndexed { index, track -> if (index == 0) track.copy(title = previewLongTrackTitle) else track }
    SearchScreen(tracks, previewAlbums, {}, {}, {}, initialQuery = "signal")
}

@PreviewTest
@Preview(name = "Playlist detail", widthDp = 412, heightDp = 915)
@Composable
fun PlaylistDetailPreview() = TerminalPreview {
    val tracks = previewTracks.mapIndexed { index, track ->
        if (index == 0) track.copy(title = previewLongTrackTitle, artist = previewLongArtist) else track
    }
    val rows = tracks.take(2).mapIndexed { index, track ->
        PlaylistEntryRow(index, track.relativePath, track.title, track.artist, track.durationMs, track.status)
    }
    PlaylistDetailScreen(
        playlistDetailState(PlaylistEntity(1, "Night Run", 1), rows, tracks),
        {}, {}, {}, {}, {}, { _, _ -> },
    )
}

@PreviewTest
@Preview(name = "Playlist dragged row", widthDp = 412, heightDp = 120)
@Composable
fun PlaylistDraggedRowPreview() = TerminalPreview {
    PlaylistTrackEntryRow(
        row = PlaylistTrackRow(
            position = 1,
            relativePath = "missing.flac",
            title = "Unavailable Carrier Across The Endless Terminal Horizon",
            artist = "UNAVAILABLE",
            available = false,
            canMoveUp = true,
            canMoveDown = true,
            track = null,
        ),
        previewIndex = 1,
        itemCount = 4,
        isDragging = true,
        dragTranslationY = 0f,
        onMove = { _, _ -> },
        onRemove = {},
        onDragStart = {},
        onDrag = {},
        onDragEnd = {},
        onDragCancel = {},
    )
}

@PreviewTest
@Preview(name = "Now playing", widthDp = 412, heightDp = 915)
@Composable
fun NowPlayingPreview() = TerminalPreview {
    NowPlayingScreen(
        PlaybackUiState(
            title = "Carrier 1",
            artist = "Signal One",
            album = "Red Horizon",
            albumId = "red",
            positionMs = 72_000,
            durationMs = 183_000,
            playing = true,
            shuffle = true,
            repeatMode = Player.REPEAT_MODE_ALL,
            currentOccurrenceId = "current",
            currentPath = "red/01.flac",
            upNext = listOf(PlaybackQueueItem("next", "next.flac", "Afterimage", "Signal One")),
        ),
        previewAlbums.first(),
        true,
        {}, {}, {}, {}, {}, {},
        currentTrackFavorite = true,
        currentTrackPlayCount = 7,
    )
}

@PreviewTest
@Preview(name = "Queue editor", widthDp = 412, heightDp = 915)
@Composable
fun QueueEditorPreview() = TerminalPreview {
    QueueEditorScreen(
        state = queueEditorState(
            current = QueueEntry("current", "red/01.flac", previewLongTrackTitle, "Signal One", "Red Horizon", 183_000),
            upcoming = listOf(
                QueueEntry("next-1", "red/02.flac", previewLongTrackTitle, "Signal One", "Red Horizon", 241_000),
                QueueEntry("next-2", "red/02.flac", "Afterimage 1", "Signal One", "Red Horizon", 241_000),
            ),
            canUndo = true,
            notice = "REMOVED CARRIER 2",
        ),
        {}, {}, { _, _, _ -> }, {}, {}, {}, {},
    )
}
private const val previewLongTrackTitle = "Carrier Across The Endless Terminal Horizon Repeating Forever"
private const val previewLongArtist = "The Extremely Long Terminal Ensemble Beyond The Horizon"

@PreviewTest
@Preview(name = "Visualizer sync controls", widthDp = 412, heightDp = 412)
@Composable
fun VisualizerSyncControlsPreview() = TerminalPreview {
    Box(Modifier.size(392.dp)) {
        TerminalVisualizerScene(
            mode = VisualizerDisplayMode.RADAR,
            frame = radarFrame,
            effectsEnabled = true,
            modifier = Modifier.fillMaxSize(),
        )
        VisualizerSyncControls(
            syncOffsetMs = 150,
            onDecrease = {},
            onIncrease = {},
            onReset = {},
            modifier = Modifier.fillMaxSize(),
            labelVisible = true,
            labelAlpha = 1f,
        )
    }
}

@PreviewTest
@Preview(name = "Queue dragged row", widthDp = 412, heightDp = 120)
@Composable
fun QueueDraggedRowPreview() = TerminalPreview {
    UpcomingQueueRow(
        row = QueueEditorRow(
            track = QueueEditorTrack("next-2", "Afterimage 1", "Signal One", "Red Horizon", 241_000),
            upcomingIndex = 1,
            canMoveUp = true,
            canMoveDown = true,
        ),
        currentOccurrenceId = "current",
        isDragging = true,
        dragTranslationY = 0f,
        itemCount = 4,
        onJump = {},
        onMove = { _, _, _ -> },
        onRemove = {},
        onDragStart = {},
        onDrag = {},
        onDragEnd = {},
        onDragCancel = {},
        modifier = Modifier.padding(16.dp),
    )
}

@Preview(name = "Visualizer radar", widthDp = 320, heightDp = 320)
@Composable
fun VisualizerRadarPreview() = TerminalPreview {
    TerminalVisualizerScene(VisualizerDisplayMode.RADAR, radarFrame, true, Modifier.fillMaxSize())
}

@Preview(name = "Visualizer bands", widthDp = 320, heightDp = 320)
@Composable
fun VisualizerBandsPreview() = TerminalPreview {
    TerminalVisualizerScene(VisualizerDisplayMode.BANDS, spectrumFrame, true, Modifier.fillMaxSize())
}

@Preview(name = "Visualizer radar idle", widthDp = 320, heightDp = 320)
@Composable
fun VisualizerRadarIdlePreview() = TerminalPreview {
    TerminalVisualizerScene(VisualizerDisplayMode.RADAR, AudioAnalysisFrame.Idle, true, Modifier.fillMaxSize())
}

@Preview(name = "Visualizer unavailable", widthDp = 320, heightDp = 320)
@Composable
fun VisualizerUnavailablePreview() = TerminalPreview {
    TerminalVisualizerScene(VisualizerDisplayMode.BANDS, AudioAnalysisFrame.Unavailable, true, Modifier.fillMaxSize())
}

@PreviewTest
@Preview(name = "Settings effects off", widthDp = 412, heightDp = 915)
@Composable
fun SettingsPreview() = TerminalPreview {
    SettingsScreen({}, {}, TerminalSettingsState(savedEffectsEnabled = false, effectiveEffectsEnabled = false), {})
}
