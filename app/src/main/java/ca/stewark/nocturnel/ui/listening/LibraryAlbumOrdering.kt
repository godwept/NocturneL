package ca.stewark.nocturnel.ui.listening

import ca.stewark.nocturnel.data.entity.AlbumEntity

internal fun orderLibraryAlbums(
    albums: List<AlbumEntity>,
    favoriteAlbumIds: Set<String>,
    sortMode: LibrarySortMode,
    albumPlayCounts: Map<String, Long>,
): List<AlbumEntity> {
    val artistThenTitle = compareBy<AlbumEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }
        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    val modeComparator = when (sortMode) {
        LibrarySortMode.ARTIST -> artistThenTitle
        LibrarySortMode.TITLE -> compareBy<AlbumEntity, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
        LibrarySortMode.YEAR -> compareBy<AlbumEntity> { parseYear(it) == null }
            .thenByDescending { parseYear(it) ?: Int.MIN_VALUE }
            .then(artistThenTitle)
        LibrarySortMode.MOST_PLAYED -> compareByDescending<AlbumEntity> { albumPlayCounts[it.id] ?: 0L }
            .then(artistThenTitle)
    }
    return albums.sortedWith(
        compareBy<AlbumEntity> { it.id !in favoriteAlbumIds }.then(modeComparator),
    )
}

private fun parseYear(album: AlbumEntity): Int? = album.year?.trim()?.toIntOrNull()
