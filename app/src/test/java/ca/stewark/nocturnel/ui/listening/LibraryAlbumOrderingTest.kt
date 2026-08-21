package ca.stewark.nocturnel.ui.listening

import ca.stewark.nocturnel.data.entity.AlbumEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryAlbumOrderingTest {
    @Test fun `favorites lead in every sort mode`() {
        val albums = listOf(
            album("regular-first", "Alpha", "Alpha", "2026"),
            album("favorite-last", "Zulu", "Zulu", "1990"),
        )

        LibrarySortMode.entries.forEach { mode ->
            assertEquals(
                listOf("favorite-last", "regular-first"),
                orderLibraryAlbums(
                    albums,
                    setOf("favorite-last"),
                    mode,
                    mapOf("regular-first" to 100, "favorite-last" to 0),
                ).map { it.id },
            )
        }
    }

    @Test fun `artist sorts by artist then title ignoring case`() {
        val albums = listOf(
            album("b", "beta", "Signal"),
            album("z", "Zulu", "alpha"),
            album("a", "Alpha", "signal"),
        )

        assertEquals(
            listOf("z", "a", "b"),
            orderLibraryAlbums(albums, emptySet(), LibrarySortMode.ARTIST, emptyMap()).map { it.id },
        )
    }

    @Test fun `title sorts by title then artist ignoring case`() {
        val albums = listOf(
            album("z", "Signal", "Zulu"),
            album("a", "signal", "alpha"),
            album("first", "Alpha", "Middle"),
        )

        assertEquals(
            listOf("first", "a", "z"),
            orderLibraryAlbums(albums, emptySet(), LibrarySortMode.TITLE, emptyMap()).map { it.id },
        )
    }

    @Test fun `year sorts newest first with invalid values last and artist title tie breaks`() {
        val albums = listOf(
            album("unknown", "Unknown", "Zero", null),
            album("older", "Older", "Beta", " 2020 "),
            album("new-z", "Zulu", "Zulu", "2026"),
            album("invalid", "Invalid", "Alpha", "later"),
            album("new-a", "Alpha", "alpha", "2026"),
            album("blank", "Blank", "Middle", " "),
        )

        assertEquals(
            listOf("new-a", "new-z", "older", "invalid", "blank", "unknown"),
            orderLibraryAlbums(albums, emptySet(), LibrarySortMode.YEAR, emptyMap()).map { it.id },
        )
    }

    @Test fun `most played sorts descending with missing counts treated as zero`() {
        val albums = listOf(
            album("missing-z", "Zulu", "Zulu"),
            album("popular", "Popular", "Middle"),
            album("missing-a", "Alpha", "alpha"),
            album("zero", "Zero", "Beta"),
        )

        assertEquals(
            listOf("popular", "missing-a", "zero", "missing-z"),
            orderLibraryAlbums(
                albums,
                emptySet(),
                LibrarySortMode.MOST_PLAYED,
                mapOf("popular" to 9, "zero" to 0),
            ).map { it.id },
        )
    }

    @Test fun `equal keys retain source order`() {
        val albums = listOf(
            album("first", "Signal", "Artist", "2026"),
            album("second", "signal", "artist", "2026"),
        )

        LibrarySortMode.entries.forEach { mode ->
            assertEquals(
                listOf("first", "second"),
                orderLibraryAlbums(albums, emptySet(), mode, emptyMap()).map { it.id },
            )
        }
    }

    private fun album(id: String, title: String, artist: String, year: String? = null) = AlbumEntity(
        id = id,
        relativeFolder = id,
        title = title,
        artist = artist,
        year = year,
        manualArtworkUri = null,
        folderArtworkUri = null,
        embeddedArtwork = null,
    )
}
