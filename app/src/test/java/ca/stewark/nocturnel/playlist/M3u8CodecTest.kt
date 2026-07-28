package ca.stewark.nocturnel.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3u8CodecTest {
    @Test
    fun importsRelativeEntriesAndReportsAbsoluteOrUnknownEntries() {
        val result = M3u8Codec.parse("#EXTM3U\nArtist/Album/01.mp3\n/elsewhere.mp3\nmissing.mp3", setOf("Artist/Album/01.mp3"))
        assertEquals(listOf("Artist/Album/01.mp3"), result.paths)
        assertEquals(2, result.skipped.size)
    }

    @Test
    fun exportsUtf8RelativePathsInPlaylistOrder() {
        assertTrue(M3u8Codec.encode(listOf("A/02.mp3", "A/01.mp3")).endsWith("A/02.mp3\nA/01.mp3\n"))
    }
}
