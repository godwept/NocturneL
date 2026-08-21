package ca.stewark.nocturnel.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.LibrarySourceEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntryEntity
import ca.stewark.nocturnel.data.entity.ScanReportEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.playlist.PlaylistNotFoundException
import ca.stewark.nocturnel.playlist.PlaylistRepository
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
    fun albumAppendPreservesOrderAndSkipsExistingPaths() = runTest {
        val dao = database.libraryDao()
        val id = dao.createPlaylist(PlaylistEntity(name = "Test", updatedEpochMillis = 1))
        dao.replacePlaylistEntries(id, listOf(PlaylistEntryEntity(id, 0, "existing.flac")))

        val result = PlaylistRepository(dao).appendAlbum(
            id,
            listOf("01.flac", "existing.flac", "03.flac"),
        )

        assertEquals(listOf("existing.flac", "01.flac", "03.flac"), dao.playlistEntries(id).map { it.relativePath })
        assertEquals(2, result.added)
        assertEquals(1, result.skipped)
    }

    @Test fun createsPlaylistAndMissingEntriesAtomically() = runTest {
        val dao = database.libraryDao()
        val id = PlaylistRepository(dao).createWithEntries("Backup", listOf("present.flac", "missing.flac"))
        assertEquals(listOf("Backup"), dao.allPlaylists().map { it.name })
        assertEquals(listOf("present.flac", "missing.flac"), dao.playlistEntries(id).map { it.relativePath })
    }

    @Test(expected = PlaylistNotFoundException::class)
    fun albumAppendRejectsDeletedPlaylist() = runTest {
        PlaylistRepository(database.libraryDao()).appendAlbum(999, listOf("01.flac"))
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

    @Test
    fun changingLibrarySourceClearsListeningDataButSameSourcePreservesIt() = runTest {
        val source = LibrarySourceEntity(treeUri = "content://music", displayName = "Music", lastScanEpochMillis = null, accessLost = false)
        database.replaceLibrarySource(source, sourceChanged = false)
        database.listeningDao().toggleFavoriteTrack("one.flac", 1)
        database.listeningDao().recordQualifiedPlay("one", "one.flac", 2)

        database.replaceLibrarySource(source, sourceChanged = false)
        assertEquals(listOf("one.flac"), database.listeningDao().favoriteTrackPaths().first())

        database.replaceLibrarySource(source.copy(treeUri = "content://other"), sourceChanged = true)
        assertTrue(database.listeningDao().favoriteTrackPaths().first().isEmpty())
        assertTrue(database.listeningDao().history().first().isEmpty())
    }

    @Test fun completedScanPersistsTrackFingerprint() = runTest {
        val dao = database.libraryDao()
        val track = TrackEntity(
            "Artist/Album/01.mp3", "content://track/1", "album-id", "Title", "Artist", "Album",
            1_000, 1, 1, "PLAYABLE", 10, 42, 1_000,
        )

        dao.replaceCompletedScan(
            albums = listOf(album(manualArtworkUri = null)),
            tracks = listOf(track),
            report = ScanReportEntity(10, 1, 0, 0, 0, 0),
            issues = emptyList(),
        )

        assertEquals(42L, dao.track(track.relativePath)?.fileSizeBytes)
        assertEquals(1_000L, dao.track(track.relativePath)?.lastModifiedEpochMillis)
    }

    @Test
    fun successfulReplacementCommitsSourceCatalogAndListeningResetTogether() = runTest {
        val dao = database.libraryDao()
        val oldSource = LibrarySourceEntity(treeUri = "content://old", displayName = "Old", lastScanEpochMillis = 1, accessLost = false)
        database.replaceLibrarySource(oldSource, sourceChanged = false)
        dao.saveAlbum(album(manualArtworkUri = null, title = "Old album"))
        database.listeningDao().toggleFavoriteTrack("old.flac", 1)

        val newSource = oldSource.copy(treeUri = "content://new", displayName = "New", lastScanEpochMillis = 2)
        database.saveSourceAndCompletedScan(
            source = newSource,
            albums = listOf(album(manualArtworkUri = null, title = "New album")),
            tracks = emptyList(),
            report = ScanReportEntity(2, 0, 0, 0, 0, 0),
            issues = emptyList(),
            sourceChanged = true,
        )

        assertEquals("content://new", dao.librarySource()?.treeUri)
        assertEquals("New album", dao.album("album-id")?.title)
        assertTrue(database.listeningDao().favoriteTrackPaths().first().isEmpty())
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
