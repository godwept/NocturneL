package ca.stewark.nocturnel.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.LibrarySourceEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NocturneLDatabaseTest {
    private lateinit var database: NocturneLDatabase

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            NocturneLDatabase::class.java,
        ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun completedScanPreservesManualArtwork() = runTest {
        val dao = database.libraryDao()
        val initial = album(manualArtworkUri = "content://manual")
        dao.saveAlbum(initial)

        dao.saveAlbumsPreservingManualArtwork(listOf(album(manualArtworkUri = null, title = "Updated title")))

        assertEquals("content://manual", dao.album(initial.id)?.manualArtworkUri)
        assertEquals("Updated title", dao.album(initial.id)?.title)
    }

    @Test
    fun deletingPlaylistAlsoDeletesItsEntries() = runTest {
        val dao = database.libraryDao()
        val id = dao.createPlaylist(PlaylistEntity(name = "Test", updatedEpochMillis = 1))
        dao.replacePlaylistEntries(id, listOf(PlaylistEntryEntity(id, 0, "missing.mp3")))

        dao.deletePlaylistAndEntries(id)

        assertTrue(dao.playlistEntries(id).isEmpty())
    }

    @Test
    fun changingLibrarySourceClearsTheOldCatalogButKeepsPlaylists() = runTest {
        val dao = database.libraryDao()
        dao.saveAlbum(album(manualArtworkUri = null))
        val playlistId = dao.createPlaylist(PlaylistEntity(name = "Portable", updatedEpochMillis = 1))

        dao.selectLibrarySource(
            LibrarySourceEntity(treeUri = "content://new", displayName = "New", lastScanEpochMillis = null, accessLost = false),
            sourceChanged = true,
        )

        assertEquals(null, dao.album("album-id"))
        assertEquals(listOf(playlistId), dao.playlists().first().map { it.id })
    }

    private fun album(manualArtworkUri: String?, title: String = "Album") = AlbumEntity(
        id = "album-id",
        relativeFolder = "Artist/Album",
        title = title,
        artist = "Artist",
        year = null,
        manualArtworkUri = manualArtworkUri,
        folderArtworkUri = null,
        embeddedArtwork = null,
    )
}
