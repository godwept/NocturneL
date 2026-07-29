package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.ui.artwork.RetroArtwork
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.playlist.AlbumPlaylistUiState
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun AlbumDetailScreen(
    album: AlbumEntity,
    tracks: List<TrackEntity>,
    onBack: () -> Unit,
    onPlay: (TrackEntity) -> Unit,
    onPlayAlbum: (List<TrackEntity>) -> Unit,
    onChooseArtwork: () -> Unit,
    onClearArtwork: () -> Unit,
    onShuffleAlbum: (List<TrackEntity>) -> Unit = { onPlayAlbum(it.shuffled()) },
    playlists: List<PlaylistEntity> = emptyList(),
    playlistPickerExpanded: Boolean = false,
    albumPlaylistState: AlbumPlaylistUiState = AlbumPlaylistUiState.Idle,
    onTogglePlaylistPicker: () -> Unit = {},
    onAddAlbumToPlaylist: (PlaylistEntity) -> Unit = {},
    onCreatePlaylistAndAdd: (String) -> Unit = {},
) {
    val playableTracks = tracks.filter { it.status == "PLAYABLE" }
    Column(Modifier.fillMaxSize().padding(TerminalDimensions.sm)) {
        Row {
            BracketButton("BACK", onBack)
            BracketButton("PLAY", { onPlayAlbum(tracks) }, enabled = tracks.isNotEmpty())
            BracketButton("SHUFFLE", { onShuffleAlbum(tracks) }, enabled = tracks.isNotEmpty())
        }
        LazyColumn {
            item {
                AsciiFrame(album.title, Modifier.padding(top = TerminalDimensions.xs)) {
                    RetroArtwork(album, Modifier.fillMaxWidth().aspectRatio(1f))
                    Text(album.artist, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = TerminalDimensions.xs))
                    album.year?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
                    Row {
                        BracketButton("SET COVER", onChooseArtwork)
                        if (album.manualArtworkUri != null) BracketButton("CLEAR", onClearArtwork)
                    }
                    BracketButton(
                        "ADD TO PLAYLIST",
                        onTogglePlaylistPicker,
                        enabled = playableTracks.isNotEmpty(),
                        selected = playlistPickerExpanded,
                    )
                }
            }
            if (playlistPickerExpanded) {
                item {
                    AlbumPlaylistPicker(
                        playlists = playlists,
                        state = albumPlaylistState,
                        onPlaylistSelected = onAddAlbumToPlaylist,
                        onCreateAndAdd = onCreatePlaylistAndAdd,
                        modifier = Modifier.padding(top = TerminalDimensions.xs),
                    )
                }
            } else if (albumPlaylistState is AlbumPlaylistUiState.Success) {
                item { AlbumPlaylistFeedback(albumPlaylistState, Modifier.padding(TerminalDimensions.xs)) }
            }
            items(tracks, key = { it.relativePath }) { track ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = TerminalDimensions.minimumTouchTarget)
                        .clickable { onPlay(track) }
                        .padding(horizontal = TerminalDimensions.xs, vertical = TerminalDimensions.sm),
                ) {
                    Text(track.trackNumber?.toString()?.padStart(2, '0') ?: "--", color = MaterialTheme.colorScheme.secondary)
                    Text(track.title, Modifier.weight(1f).padding(horizontal = TerminalDimensions.xs))
                    Text(formatDuration(track.durationMs), color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
