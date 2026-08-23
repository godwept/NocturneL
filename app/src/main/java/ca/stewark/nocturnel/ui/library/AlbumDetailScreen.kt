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
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.ui.artwork.RetroArtwork
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.QueueTrackActions
import ca.stewark.nocturnel.ui.components.FavoriteToggle
import ca.stewark.nocturnel.ui.components.TerminalActionRow
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
    onShuffleAlbum: (List<TrackEntity>) -> Unit = { onPlayAlbum(it.shuffled()) },
    playlists: List<PlaylistEntity> = emptyList(),
    playlistPickerExpanded: Boolean = false,
    albumPlaylistState: AlbumPlaylistUiState = AlbumPlaylistUiState.Idle,
    onTogglePlaylistPicker: () -> Unit = {},
    onAddAlbumToPlaylist: (PlaylistEntity) -> Unit = {},
    onCreatePlaylistAndAdd: (String) -> Unit = {},
    onAddAlbumToQueue: (List<TrackEntity>) -> Unit = {},
    onAddTrackToQueue: (TrackEntity) -> Unit = {},
    albumFavorite: Boolean = false,
    albumPlayCount: Long = 0,
    favoriteTrackPaths: Set<String> = emptySet(),
    trackPlayCounts: Map<String, Long> = emptyMap(),
    onToggleAlbumFavorite: (AlbumEntity) -> Unit = {},
    onToggleTrackFavorite: (TrackEntity) -> Unit = {},
) {
    val playableTracks = tracks.filter { it.status == "PLAYABLE" }
    Column(Modifier.fillMaxSize().padding(TerminalDimensions.sm)) {
        TerminalActionRow {
            BracketButton("BACK", onBack)
            BracketButton("PLAY", { onPlayAlbum(tracks) }, enabled = tracks.isNotEmpty())
            BracketButton("SHUFFLE", { onShuffleAlbum(tracks) }, enabled = tracks.isNotEmpty())
            BracketButton("ADD QUEUE", { onAddAlbumToQueue(tracks) }, enabled = playableTracks.isNotEmpty())
        }
        LazyColumn {
            item {
                AsciiFrame(album.title, Modifier.padding(top = TerminalDimensions.xs)) {
                    RetroArtwork(album, Modifier.fillMaxWidth().aspectRatio(1f))
                    Text(album.artist, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = TerminalDimensions.xs))
                    album.year?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
                    Text("$albumPlayCount PLAY(S)", color = MaterialTheme.colorScheme.secondary)
                    TerminalActionRow {
                        FavoriteToggle(album.title, albumFavorite, { onToggleAlbumFavorite(album) })
                        BracketButton("SET COVER", onChooseArtwork)
                        BracketButton(
                            "ADD TO PLAYLIST",
                            onTogglePlaylistPicker,
                            enabled = playableTracks.isNotEmpty(),
                            selected = playlistPickerExpanded,
                        )
                    }
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
                        .padding(horizontal = TerminalDimensions.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(track.trackNumber?.toString()?.padStart(2, '0') ?: "--", color = MaterialTheme.colorScheme.secondary)
                    Text(
                        track.title,
                        Modifier.weight(1f).padding(horizontal = TerminalDimensions.xs),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("${trackPlayCounts[track.relativePath] ?: 0}×", color = MaterialTheme.colorScheme.secondary)
                    Text(formatDuration(track.durationMs), color = MaterialTheme.colorScheme.secondary)
                    FavoriteToggle(track.title, track.relativePath in favoriteTrackPaths, { onToggleTrackFavorite(track) })
                    if (track.status == "PLAYABLE") {
                        QueueTrackActions(track.title, { onAddTrackToQueue(track) })
                    }
                }
            }
        }
    }
}
