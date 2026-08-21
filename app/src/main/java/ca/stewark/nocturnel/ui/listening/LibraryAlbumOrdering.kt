package ca.stewark.nocturnel.ui.listening

import ca.stewark.nocturnel.data.entity.AlbumEntity

internal fun orderLibraryAlbums(
    albums: List<AlbumEntity>,
    favoriteAlbumIds: Set<String>,
): List<AlbumEntity> = albums.sortedWith(
    compareBy<AlbumEntity> { it.id !in favoriteAlbumIds }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
)
