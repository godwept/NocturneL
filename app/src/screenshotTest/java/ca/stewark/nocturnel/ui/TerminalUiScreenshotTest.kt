package ca.stewark.nocturnel.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.data.model.PlaylistEntryRow
import ca.stewark.nocturnel.data.model.ListeningHistoryRow
import ca.stewark.nocturnel.playback.PlaybackQueueItem
import ca.stewark.nocturnel.playback.PlaybackUiState
import ca.stewark.nocturnel.playback.QueueEntry
import ca.stewark.nocturnel.ui.library.AlbumDetailScreen
import ca.stewark.nocturnel.ui.library.AlbumGridScreen
import ca.stewark.nocturnel.ui.library.ArtistsScreen
import ca.stewark.nocturnel.ui.library.LibrarySetupScreen
import ca.stewark.nocturnel.ui.library.SearchScreen
import ca.stewark.nocturnel.ui.listening.FavoritesScreen
import ca.stewark.nocturnel.ui.listening.LibraryLandingScreen
import ca.stewark.nocturnel.ui.listening.ListeningHistoryScreen
import ca.stewark.nocturnel.ui.listening.ListeningUiState
import ca.stewark.nocturnel.ui.listening.ResumeUiState
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
import ca.stewark.nocturnel.ui.playlist.playlistDetailState
import ca.stewark.nocturnel.ui.settings.SettingsScreen
import ca.stewark.nocturnel.ui.settings.TerminalSettingsState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import ca.stewark.nocturnel.ui.components.TerminalScaffold
import ca.stewark.nocturnel.ui.navigation.NocturneLDestination
import com.android.tools.screenshot.PreviewTest
import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import kotlin.math.PI
import kotlin.math.sin

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
private val previewHistory = previewTracks.take(4).mapIndexed { index, track ->
    ListeningHistoryRow(
        id = index.toLong(), qualificationId = "preview-$index", relativePath = track.relativePath,
        playedAtEpochMillis = 1_786_000_000_000L - index * 60_000L,
        title = track.title, artist = track.artist, album = track.album, albumId = track.albumId,
        durationMs = track.durationMs, status = track.status, documentUri = track.documentUri,
    )
}
private val previewListening = ListeningUiState(
    favoriteTrackPaths = previewTracks.take(3).mapTo(mutableSetOf()) { it.relativePath },
    favoriteAlbumIds = previewAlbums.take(3).mapTo(mutableSetOf()) { it.id },
    favoriteTracks = previewTracks.take(3),
    favoriteAlbums = previewAlbums.take(3),
    trackPlayCounts = previewTracks.associate { it.relativePath to (it.trackNumber ?: 0).toLong() * 4 },
    albumPlayCounts = previewAlbums.associate { it.id to 12L },
    history = previewHistory,
    recentTracks = previewHistory,
)

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
private val tunnelFrame = radarFrame.copy(
    waveform = List(128) { index -> (sin(2.0 * PI * index / 24.0) * .8).toFloat() },
    lowEnergy = .55f,
    midEnergy = .62f,
    highEnergy = .38f,
    transient = 0f,
    frameId = 43,
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
            previewAlbums, previewListening,
            ResumeUiState("Carrier 1", "Signal One", 72_000, 183_000, true),
            rememberLazyGridState(), {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@PreviewTest
@Preview(name = "Favorites", widthDp = 412, heightDp = 915)
@Composable
fun FavoritesPreview() = TerminalPreview { FavoritesScreen(previewListening, {}, {}, {}, {}, {}) }

@PreviewTest
@Preview(name = "History", widthDp = 412, heightDp = 915)
@Composable
fun HistoryPreview() = TerminalPreview { ListeningHistoryScreen(previewListening, {}, {}, {}, formatTimestamp = { "AUG 18 · 21:00" }) }

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
    AlbumDetailScreen(
        album = previewAlbums.first(),
        tracks = previewTracks.take(2),
        onBack = {},
        onPlay = {},
        onPlayAlbum = {},
        onChooseArtwork = {},
        onClearArtwork = {},
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
    AlbumDetailScreen(
        album = previewAlbums.first(),
        tracks = previewTracks.take(2),
        onBack = {},
        onPlay = {},
        onPlayAlbum = {},
        onChooseArtwork = {},
        onClearArtwork = {},
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
    SearchScreen(previewTracks, previewAlbums, {}, {}, {}, initialQuery = "signal")
}

@PreviewTest
@Preview(name = "Playlist detail", widthDp = 412, heightDp = 915)
@Composable
fun PlaylistDetailPreview() = TerminalPreview {
    val rows = previewTracks.take(2).mapIndexed { index, track ->
        PlaylistEntryRow(index, track.relativePath, track.title, track.artist, track.durationMs, track.status)
    }
    PlaylistDetailScreen(
        playlistDetailState(PlaylistEntity(1, "Night Run", 1), rows, previewTracks),
        {}, {}, {}, {}, {}, { _, _ -> },
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
            currentOccurrenceId = "current",
            currentPath = "red/01.flac",
            upNext = listOf(PlaybackQueueItem("next", "next.flac", "Afterimage", "Signal One")),
        ),
        previewAlbums.first(),
        true,
        {}, {}, {}, {}, {}, {},
    )
}

@PreviewTest
@Preview(name = "Queue editor", widthDp = 412, heightDp = 915)
@Composable
fun QueueEditorPreview() = TerminalPreview {
    QueueEditorScreen(
        state = queueEditorState(
            current = QueueEntry("current", "red/01.flac", "Carrier 1", "Signal One", "Red Horizon", 183_000),
            upcoming = listOf(
                QueueEntry("next-1", "red/02.flac", "Afterimage 1", "Signal One", "Red Horizon", 241_000),
                QueueEntry("next-2", "red/02.flac", "Afterimage 1", "Signal One", "Red Horizon", 241_000),
            ),
            canUndo = true,
            notice = "REMOVED CARRIER 2",
        ),
        {}, {}, { _, _, _ -> }, {}, {}, {}, {},
    )
}

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
            modifier = Modifier.align(Alignment.BottomCenter),
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

@PreviewTest
@Preview(name = "Visualizer tunnel", widthDp = 320, heightDp = 320)
@Composable
fun VisualizerTunnelPreview() = TerminalPreview {
    TerminalVisualizerScene(VisualizerDisplayMode.TUNNEL, tunnelFrame, true, Modifier.fillMaxSize())
}

@PreviewTest
@Preview(name = "Visualizer tunnel effects off", widthDp = 320, heightDp = 320)
@Composable
fun VisualizerTunnelEffectsOffPreview() = TerminalPreview {
    TerminalVisualizerScene(VisualizerDisplayMode.TUNNEL, tunnelFrame, false, Modifier.fillMaxSize())
}

@PreviewTest
@Preview(name = "Visualizer tunnel transient", widthDp = 320, heightDp = 320)
@Composable
fun VisualizerTunnelTransientPreview() = TerminalPreview {
    TerminalVisualizerScene(
        VisualizerDisplayMode.TUNNEL,
        tunnelFrame.copy(transient = 1f, frameId = 44),
        true,
        Modifier.fillMaxSize(),
    )
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
