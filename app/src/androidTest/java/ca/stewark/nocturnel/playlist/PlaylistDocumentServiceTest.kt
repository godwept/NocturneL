package ca.stewark.nocturnel.playlist

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.stewark.nocturnel.ui.playlist.PlaylistImportPayload
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDocumentServiceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val service = PlaylistDocumentService(context.contentResolver)

    @Test fun zipMagicSelectsBundleWithoutZipFilename() {
        val expected = listOf(PlaylistBundlePlaylist("Backup", listOf("missing.flac")))
        val bytes = ByteArrayOutputStream().also { PlaylistBundleCodec.encode(expected, it) }.toByteArray()
        val file = cacheFile("backup.data").apply { writeBytes(bytes) }

        val payload = service.readImport(Uri.fromFile(file)) as PlaylistImportPayload.Bundle

        assertEquals(expected, payload.result.playlists)
    }

    @Test fun plainDocumentUsesFilenameAndBundleWritingStreamsAValidZip() {
        val plain = cacheFile("Road Trip.m3u8").apply { writeText("#EXTM3U\na.flac\n") }
        val standalone = service.readImport(Uri.fromFile(plain)) as PlaylistImportPayload.Standalone
        assertEquals("Road Trip", standalone.suggestedName)
        assertTrue("a.flac" in standalone.text)

        val bundle = cacheFile("export.zip")
        val expected = listOf(PlaylistBundlePlaylist("Empty", emptyList()))
        service.writeBundle(Uri.fromFile(bundle), expected)
        assertEquals(expected, PlaylistBundleCodec.decode(bundle.inputStream()).playlists)
    }

    private fun cacheFile(name: String): File = File(context.cacheDir, name).also {
        it.delete()
        it.deleteOnExit()
    }
}
