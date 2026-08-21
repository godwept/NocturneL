package ca.stewark.nocturnel.library

import android.media.MediaMetadataRetriever
import java.io.Closeable
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidMediaMetadataReaderTest {
    @Test fun tagReadDoesNotAccessArtworkBytes() {
        val session = FakeMetadataSession()
        val tags = AndroidMediaMetadataReader(MetadataSessionFactory { session })
            .readTags("content://fixture")
            .getOrThrow()

        assertEquals("Fixture title", tags.title)
        assertEquals(0, session.artworkReads)
        assertTrue(session.closed)
    }

    @Test fun artworkReadReturnsEmbeddedBytesIndependently() {
        val session = FakeMetadataSession()
        val artwork = AndroidMediaMetadataReader(MetadataSessionFactory { session })
            .readArtwork("content://fixture")
            .getOrThrow()

        assertArrayEquals(byteArrayOf(1, 2, 3), artwork)
        assertEquals(1, session.artworkReads)
        assertTrue(session.closed)
    }

    private class FakeMetadataSession : MetadataSession, Closeable {
        var artworkReads = 0
        var closed = false

        override fun metadata(key: Int): String? = when (key) {
            MediaMetadataRetriever.METADATA_KEY_TITLE -> "Fixture title"
            MediaMetadataRetriever.METADATA_KEY_DURATION -> "1000"
            else -> null
        }

        override fun artwork(): ByteArray = byteArrayOf(1, 2, 3).also { artworkReads += 1 }

        override fun close() {
            closed = true
        }
    }
}
