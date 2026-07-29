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
import ca.stewark.nocturnel.ui.library.AlbumDetailScreen
import ca.stewark.nocturnel.ui.library.AlbumGridScreen
import ca.stewark.nocturnel.ui.library.ArtistsScreen
import ca.stewark.nocturnel.ui.library.LibrarySetupScreen
import ca.stewark.nocturnel.ui.library.SearchScreen
import ca.stewark.nocturnel.ui.playback.NowPlayingScreen
import ca.stewark.nocturnel.ui.playlist.PlaylistDetailScreen
import ca.stewark.nocturnel.ui.playlist.playlistDetailState
import ca.stewark.nocturnel.ui.settings.SettingsScreen
import ca.stewark.nocturnel.ui.settings.TerminalSettingsState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import ca.stewark.nocturnel.ui.components.TerminalScaffold
import ca.stewark.nocturnel.ui.navigation.NocturneLDestination
import com.android.tools.screenshot.PreviewTest

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
            upNext = listOf(PlaybackQueueItem("next", "Afterimage", "Signal One")),
        ),
        previewAlbums.first(),
        true,
        {}, {}, {}, {}, {}, {},
    )
}

@PreviewTest
@Preview(name = "Settings effects off", widthDp = 412, heightDp = 915)
@Composable
fun SettingsPreview() = TerminalPreview {
    SettingsScreen({}, {}, TerminalSettingsState(savedEffectsEnabled = false, effectiveEffectsEnabled = false), {})
}
