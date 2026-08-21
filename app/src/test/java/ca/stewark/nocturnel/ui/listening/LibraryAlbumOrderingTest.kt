package ca.stewark.nocturnel.ui.listening

import ca.stewark.nocturnel.data.entity.AlbumEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryAlbumOrderingTest {
    @Test fun `favorites lead and both groups sort by title ignoring case`() {
        val albums = listOf(
            album("z", "zebra"),
            album("b", "Beta"),
            album("a", "alpha"),
            album("g", "Gamma"),
        )

        assertEquals(
            listOf("b", "g", "a", "z"),
            orderLibraryAlbums(albums, setOf("g", "b")).map { it.id },
        )
    }

    @Test fun `equal titles retain source order`() {
        val albums = listOf(album("first", "Signal"), album("second", "signal"))

        assertEquals(listOf("first", "second"), orderLibraryAlbums(albums, emptySet()).map { it.id })
    }

    private fun album(id: String, title: String) = AlbumEntity(
        id = id,
        relativeFolder = id,
        title = title,
        artist = "Artist",
        year = null,
        manualArtworkUri = null,
        folderArtworkUri = null,
        embeddedArtwork = null,
    )
}
