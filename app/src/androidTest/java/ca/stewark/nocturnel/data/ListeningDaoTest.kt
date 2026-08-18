package ca.stewark.nocturnel.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListeningDaoTest {
    private lateinit var database: NocturneLDatabase

    @Before fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), NocturneLDatabase::class.java).build()
    }

    @After fun closeDatabase() = database.close()

    @Test fun favoritesRemainIndependentAndToggle() = runTest {
        val dao = database.listeningDao()
        dao.toggleFavoriteTrack("one.flac", 1)
        dao.toggleFavoriteAlbum("album", 2)
        assertEquals(listOf("one.flac"), dao.favoriteTrackPaths().first())
        assertEquals(listOf("album"), dao.favoriteAlbumIds().first())
        dao.toggleFavoriteTrack("one.flac", 3)
        assertFalse(dao.isFavoriteTrack("one.flac"))
        assertEquals(listOf("album"), dao.favoriteAlbumIds().first())
    }

    @Test fun recordIsIdempotentAndRetentionDoesNotReduceLifetimeCount() = runTest {
        val dao = database.listeningDao()
        repeat(205) { dao.recordQualifiedPlay("occurrence-$it", "one.flac", it.toLong()) }
        dao.recordQualifiedPlay("occurrence-204", "one.flac", 999)
        assertEquals(200, dao.history().first().size)
        assertEquals(205, dao.trackPlayCounts().first().single().playCount)
    }

    @Test fun projectionsHideMissingFavoritesAndDeduplicateRecent() = runTest {
        val library = database.libraryDao()
        val listening = database.listeningDao()
        library.saveAlbum(AlbumEntity("album", "album", "Album", "Artist", null, null, null, null))
        library.saveTracks(listOf(
            TrackEntity("one.flac", "content://one", "album", "One", "Artist", "Album", 100, 1, 1, "PLAYABLE", 1),
            TrackEntity("missing.flac", "content://missing", "album", "Missing", "Artist", "Album", 100, 2, 1, "MISSING", 1),
        ))
        listening.toggleFavoriteTrack("one.flac", 1)
        listening.toggleFavoriteTrack("missing.flac", 2)
        listening.recordQualifiedPlay("a", "one.flac", 10)
        listening.recordQualifiedPlay("b", "one.flac", 20)
        assertEquals(listOf("one.flac"), listening.favoriteTracks().first().map { it.relativePath })
        assertEquals(listOf("one.flac"), listening.recentDistinct(5).first().map { it.relativePath })
        assertEquals(2, listening.history().first().size)
    }

    @Test fun clearingCountsPreservesFavorites() = runTest {
        val dao = database.listeningDao()
        dao.toggleFavoriteTrack("one.flac", 1)
        dao.recordQualifiedPlay("one", "one.flac", 2)
        dao.clearHistoryAndCounts()
        assertEquals(listOf("one.flac"), dao.favoriteTrackPaths().first())
        assertEquals(emptyList<Any>(), dao.history().first())
        assertEquals(emptyList<Any>(), dao.trackPlayCounts().first())
    }
}
