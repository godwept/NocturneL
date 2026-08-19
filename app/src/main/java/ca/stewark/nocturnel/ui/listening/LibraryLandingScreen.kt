package ca.stewark.nocturnel.ui.listening

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.BracketButton
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.library.AlbumGridCard
import ca.stewark.nocturnel.ui.library.formatDuration
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun LibraryLandingScreen(
    albums: List<AlbumEntity>,
    listening: ListeningUiState,
    resume: ResumeUiState?,
    state: LazyGridState,
    onResume: () -> Unit,
    onAlbumSelected: (AlbumEntity) -> Unit,
    onTrackSelected: (TrackEntity) -> Unit,
    onFavoriteAlbum: (String) -> Unit,
    onFavoriteTrack: (String) -> Unit,
    onViewFavorites: () -> Unit,
    onViewHistory: () -> Unit,
) {
    if (albums.isEmpty() && resume == null && listening.favoriteAlbums.isEmpty() && listening.favoriteTracks.isEmpty() && listening.recentTracks.isEmpty()) {
        TerminalNotice("No playable albums yet. Rescan after adding music.", Modifier.padding(TerminalDimensions.lg))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), state = state, modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(TerminalDimensions.sm),
    ) {
        resume?.let { item(span = { GridItemSpan(maxLineSpan) }) {
            AsciiFrame("RESUME", Modifier.padding(bottom = TerminalDimensions.sm)) {
                Text(it.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(it.artist, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatDuration(it.positionMs)} / ${formatDuration(it.durationMs)}",
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BracketButton("RESUME", onResume, enabled = it.enabled)
                if (!it.enabled) TerminalNotice("ACCESS TO MUSIC FOLDER LOST")
            }
        } }
        if (listening.previewFavoriteAlbums.isNotEmpty() || listening.previewFavoriteTracks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeading("FAVORITES", "VIEW ALL FAVORITES", onViewFavorites) }
            items(listening.previewFavoriteAlbums, key = { "fav-album:${it.id}" }) { album ->
                AlbumGridCard(album, true, listening.albumPlayCounts[album.id] ?: 0, onAlbumSelected, { onFavoriteAlbum(it.id) })
            }
            items(listening.previewFavoriteTracks, key = { "fav-track:${it.relativePath}" }, span = { GridItemSpan(maxLineSpan) }) { track ->
                ListeningTrackRow(track.title, track.artist, listening.trackPlayCounts[track.relativePath] ?: 0, true, true, { onTrackSelected(track) }, { onFavoriteTrack(track.relativePath) })
            }
        }
        if (listening.previewRecentTracks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionHeading("RECENTLY PLAYED", "VIEW ALL HISTORY", onViewHistory) }
            items(listening.previewRecentTracks, key = { "recent:${it.relativePath}" }, span = { GridItemSpan(maxLineSpan) }) { row ->
                val track = row.toTrackEntityOrNull()
                ListeningTrackRow(
                    row.title ?: row.relativePath.substringAfterLast('/'), row.artist.orEmpty(),
                    listening.trackPlayCounts[row.relativePath] ?: 0, row.relativePath in listening.favoriteTrackPaths,
                    track != null, { track?.let(onTrackSelected) }, { onFavoriteTrack(row.relativePath) },
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Text("+--[ ALBUM LIBRARY ]", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = TerminalDimensions.sm)) }
        items(albums, key = { "album:${it.id}" }) { album ->
            AlbumGridCard(
                album, album.id in listening.favoriteAlbumIds, listening.albumPlayCounts[album.id] ?: 0,
                onAlbumSelected, { onFavoriteAlbum(it.id) }, Modifier.padding(TerminalDimensions.xs),
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String, action: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(Modifier.fillMaxSize(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
        Text("+--[ $title ]", color = MaterialTheme.colorScheme.secondary)
        BracketButton(action, onClick)
    }
}
