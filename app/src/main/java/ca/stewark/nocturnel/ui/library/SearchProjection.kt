package ca.stewark.nocturnel.ui.library

import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity

data class SearchProjection(
    val tracks: List<TrackEntity> = emptyList(),
    val albums: List<AlbumEntity> = emptyList(),
    val artists: List<ArtistRow> = emptyList(),
)

fun projectSearch(query: String, tracks: List<TrackEntity>, albums: List<AlbumEntity>): SearchProjection {
    val needle = query.trim()
    if (needle.isBlank()) return SearchProjection()
    fun String.matches() = contains(needle, ignoreCase = true)
    return SearchProjection(
        tracks = tracks.filter { it.title.matches() || it.artist.matches() || it.album.matches() },
        albums = albums.filter { it.title.matches() || it.artist.matches() },
        artists = groupArtists(albums).filter { it.name.matches() },
    )
}
