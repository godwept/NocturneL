package ca.stewark.nocturnel.ui.library

import ca.stewark.nocturnel.data.entity.AlbumEntity

data class ArtistRow(val name: String, val albums: List<AlbumEntity>)

fun groupArtists(albums: List<AlbumEntity>): List<ArtistRow> =
    albums.groupBy { it.artist.trim().ifBlank { "Unknown Artist" }.lowercase() }
        .values
        .map { grouped ->
            ArtistRow(
                grouped.first().artist.trim().ifBlank { "Unknown Artist" },
                grouped.sortedBy { it.title.lowercase() },
            )
        }
        .sortedBy { it.name.lowercase() }
