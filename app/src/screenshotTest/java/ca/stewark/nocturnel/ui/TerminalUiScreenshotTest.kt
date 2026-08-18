package ca.stewark.nocturnel.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
import ca.stewark.nocturnel.ui.playback.NowPlayingScreen
import ca.stewark.nocturnel.ui.playback.QueueEditorScreen
import ca.stewark.nocturnel.ui.playback.queueEditorState
import ca.stewark.nocturnel.ui.playback.visualizer.TerminalVisualizerScene
import ca.stewark.nocturnel.ui.playback.visualizer.VisualizerDisplayMode
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
private val scopeFrame = radarFrame.copy(
    waveform = List(128) { index -> (sin(2.0 * PI * index / 24.0) * .8).toFloat() },
    transient = .2f,
    frameId = 43,
)

@Composable
private fun TerminalPreview(content: @Composable () -> Unit) = NocturneLTheme {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
}

@PreviewTest
@Preview(name = "Album grid", widthDp = 412, heightDp = 915)
@Composable
fun AlbumGridPreview() = TerminalPreview { AlbumGridScreen(previewAlbums) {} }

@PreviewTest
@Preview(name = "Root effects off", widthDp = 412, heightDp = 915)
@Composable
fun RootPreview() = TerminalPreview {
    TerminalScaffold(NocturneLDestination.LIBRARY, {}, effectsEnabled = false, status = "LIBRARY READY") {
        LibraryScreen(previewAlbums) {}
    }
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

@Preview(name = "Visualizer scope", widthDp = 320, heightDp = 320)
@Composable
fun VisualizerScopePreview() = TerminalPreview {
    TerminalVisualizerScene(VisualizerDisplayMode.SCOPE, scopeFrame, true, Modifier.fillMaxSize())
}

@Preview(name = "Visualizer scope effects off", widthDp = 320, heightDp = 320)
@Composable
fun VisualizerScopeEffectsOffPreview() = TerminalPreview {
    TerminalVisualizerScene(VisualizerDisplayMode.SCOPE, scopeFrame, false, Modifier.fillMaxSize())
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
