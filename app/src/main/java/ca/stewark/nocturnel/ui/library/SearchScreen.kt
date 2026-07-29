package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.ui.components.AsciiFrame
import ca.stewark.nocturnel.ui.components.TerminalNotice
import ca.stewark.nocturnel.ui.components.TerminalTextField
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

@Composable
fun SearchScreen(
    tracks: List<TrackEntity>,
    albums: List<AlbumEntity>,
    onPlay: (TrackEntity) -> Unit,
    onAlbumSelected: (AlbumEntity) -> Unit,
    onArtistSelected: (ArtistRow) -> Unit,
    initialQuery: String = "",
) {
    var query by remember { mutableStateOf(initialQuery) }
    val results = projectSearch(query, tracks, albums)
    Column(Modifier.fillMaxSize().padding(TerminalDimensions.md)) {
        AsciiFrame("SEARCH") {
            TerminalTextField(query, { query = it }, "SEARCH LOCAL LIBRARY")
        }
        LazyColumn(Modifier.padding(top = TerminalDimensions.xs)) {
            if (query.isNotBlank() && results.tracks.isEmpty() && results.albums.isEmpty() && results.artists.isEmpty()) {
                item { TerminalNotice("No local matches.") }
            }
            if (results.albums.isNotEmpty()) item { GroupHeading("ALBUMS") }
            items(results.albums, key = { "album:${it.id}" }) { album -> ResultRow("${album.artist} :: ${album.title}") { onAlbumSelected(album) } }
            if (results.artists.isNotEmpty()) item { GroupHeading("ARTISTS") }
            items(results.artists, key = { "artist:${it.name}" }) { artist -> ResultRow("${artist.name} :: ${artist.albums.size} ALBUM(S)") { onArtistSelected(artist) } }
            if (results.tracks.isNotEmpty()) item { GroupHeading("TRACKS") }
            items(results.tracks, key = { "track:${it.relativePath}" }) { track -> ResultRow("${track.artist} :: ${track.title}") { onPlay(track) } }
        }
    }
}

@Composable private fun GroupHeading(text: String) =
    Text("+--[ $text ]", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = TerminalDimensions.md))

@Composable private fun ResultRow(text: String, onClick: () -> Unit) =
    Text(
        text,
        Modifier.fillMaxWidth().defaultMinSize(minHeight = TerminalDimensions.minimumTouchTarget)
            .clickable(onClick = onClick).padding(vertical = TerminalDimensions.sm),
    )
