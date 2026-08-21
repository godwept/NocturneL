package ca.stewark.nocturnel.library

import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity

data class ExistingCatalogSnapshot(
    val tracksByPath: Map<String, TrackEntity>,
    val albumsById: Map<String, AlbumEntity>,
) {
    companion object {
        val Empty = ExistingCatalogSnapshot(emptyMap(), emptyMap())

        fun from(albums: List<AlbumEntity>, tracks: List<TrackEntity>) = ExistingCatalogSnapshot(
            tracksByPath = tracks.associateBy(TrackEntity::relativePath),
            albumsById = albums.associateBy(AlbumEntity::id),
        )
    }
}
