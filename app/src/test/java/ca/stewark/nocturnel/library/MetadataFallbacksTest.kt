package ca.stewark.nocturnel.library

import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataFallbacksTest {
    @Test
    fun derivesTrackNumberAndTitleFromNumberedFilename() {
        val metadata = MetadataFallbacks.fromPath("Artist/Album/01 - Title.mp3")
        assertEquals("Title", metadata.title)
        assertEquals(1, metadata.trackNumber)
        assertEquals("Album", metadata.album)
        assertEquals("Artist", metadata.artist)
    }
}
