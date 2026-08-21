package ca.stewark.nocturnel.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.LibrarySourceEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.library.ExistingCatalogSnapshot
import ca.stewark.nocturnel.library.LibraryScanEngine
import ca.stewark.nocturnel.library.ScanOutcome
import ca.stewark.nocturnel.library.ScanProgress
import ca.stewark.nocturnel.library.ScanResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogRepositoryIncrementalTest {
    private lateinit var database: NocturneLDatabase
    private lateinit var dao: ca.stewark.nocturnel.data.dao.LibraryDao
    private lateinit var scanner: FakeScanEngine
    private lateinit var repository: CatalogRepository

    @Before fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            NocturneLDatabase::class.java,
        ).build()
        dao = database.libraryDao()
        dao.saveLibrarySource(source)
        dao.saveAlbum(album)
        dao.saveTracks(listOf(track))
        scanner = FakeScanEngine(ScanResult(listOf(album), listOf(track), emptyList(), 0, 0, ScanOutcome.COMPLETED))
        repository = CatalogRepository(database, scanner)
    }

    @After fun tearDown() = database.close()

    @Test fun sameSourceRescanPassesStoredTracksAndAlbumsToScanner() = runTest {
        repository.rescan()

        assertEquals(listOf(track), scanner.capturedSnapshot.tracksByPath.values.toList())
        assertEquals(listOf(album), scanner.capturedSnapshot.albumsById.values.toList())
    }

    @Test fun replacementSourcePassesAnEmptySnapshot() = runTest {
        repository.scanSelectedSource("content://new", "New")

        assertSame(ExistingCatalogSnapshot.Empty, scanner.capturedSnapshot)
    }

    @Test fun cancelledScanLeavesStoredCatalogUnchanged() = runTest {
        scanner.result = ScanResult(emptyList(), emptyList(), emptyList(), 0, 0, ScanOutcome.CANCELLED)

        var cancelled = false
        try {
            repository.rescan()
        } catch (_: ScanCancelledException) {
            cancelled = true
        }

        assertTrue(cancelled)
        assertEquals(source.treeUri, dao.librarySource()?.treeUri)
        assertEquals(album, dao.album(album.id))
        assertEquals(track, dao.track(track.relativePath))
    }

    private class FakeScanEngine(var result: ScanResult) : LibraryScanEngine {
        var capturedSnapshot: ExistingCatalogSnapshot = ExistingCatalogSnapshot.Empty
        override fun canAccess(treeUri: String) = true
        override fun scan(
            treeUri: String,
            scanEpochMillis: Long,
            cancelled: () -> Boolean,
            onProgress: (ScanProgress) -> Unit,
            existingCatalog: ExistingCatalogSnapshot,
        ): ScanResult {
            capturedSnapshot = existingCatalog
            return result
        }
    }

    private companion object {
        val source = LibrarySourceEntity(treeUri = "content://music", displayName = "Music", lastScanEpochMillis = 1, accessLost = false)
        val album = AlbumEntity("album", "Artist/Album", "Album", "Artist", null, null, null, null)
        val track = TrackEntity(
            "Artist/Album/01.mp3", "content://track/1", "album", "Title", "Artist", "Album",
            1_000, 1, 1, "PLAYABLE", 1, 42, 1_000,
        )
    }
}
