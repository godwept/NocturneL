package ca.stewark.nocturnel.ui.library

import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryProjectionTest {
    private fun album(id: String, title: String, artist: String) =
        AlbumEntity(id, id, title, artist, null, null, null, null)
    private fun track(title: String, artist: String, album: String) =
        TrackEntity(title, "uri:$title", album, title, artist, album, 1, 1, 1, "PLAYABLE", 1)

    @Test fun `artists group case insensitively and unknown is explicit`() {
        val rows = groupArtists(listOf(album("1", "One", "Muse"), album("2", "Two", "muse"), album("3", "Three", "")))
        assertEquals(listOf("Muse", "Unknown Artist"), rows.map { it.name })
        assertEquals(2, rows.first().albums.size)
    }

    @Test fun `search separates albums artists and tracks`() {
        val album = album("a", "Origin", "Muse")
        val projection = projectSearch("muse", listOf(track("Song", "Muse", "Origin")), listOf(album))
        assertEquals(1, projection.tracks.size)
        assertEquals(1, projection.albums.size)
        assertEquals(1, projection.artists.size)
        assertTrue(projectSearch("", emptyList(), emptyList()).tracks.isEmpty())
    }
}
