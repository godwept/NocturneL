package ca.stewark.nocturnel.library

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentFileEnumeratorTest {
    private lateinit var root: File

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.cacheDir, "document-enumerator-test").also { it.deleteRecursively() }
    }

    @After fun tearDown() {
        root.deleteRecursively()
    }

    @Test fun enumeratorReturnsPathUriAndAudioFingerprint() {
        val album = File(root, "Artist/Album").also { it.mkdirs() }
        File(album, "01.mp3").writeBytes(byteArrayOf(1, 2, 3))
        File(album, "notes.txt").writeText("notes")
        val documentRoot = DocumentFile.fromFile(root)

        val result = DocumentFileEnumerator { documentRoot }.enumerate("content://tree") { false }

        assertEquals(listOf("Artist/Album/01.mp3", "Artist/Album/notes.txt"), result.map { it.relativePath }.sorted())
        val audio = result.single { it.displayName == "01.mp3" }
        val notes = result.single { it.displayName == "notes.txt" }
        assertEquals(3L, audio.fileSizeBytes)
        assertNotNull(audio.lastModifiedEpochMillis)
        assertNull(notes.fileSizeBytes)
        assertNull(notes.lastModifiedEpochMillis)
    }

    @Test fun cancellationStopsBeforeChildrenAreReturned() {
        root.mkdirs()
        File(root, "01.mp3").writeBytes(byteArrayOf(1))
        val documentRoot = DocumentFile.fromFile(root)

        assertEquals(emptyList<DiscoveredDocument>(), DocumentFileEnumerator { documentRoot }.enumerate("tree") { true })
    }
}
