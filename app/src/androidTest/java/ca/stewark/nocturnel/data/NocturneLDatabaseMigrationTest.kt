package ca.stewark.nocturnel.data

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NocturneLDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NocturneLDatabase::class.java,
    )

    @After fun deleteDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    @Test fun migrationPreservesVersionOneDataAndCreatesListeningTables() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL("INSERT INTO library_source(id, treeUri, displayName, lastScanEpochMillis, accessLost) VALUES(0, 'content://music', 'Music', NULL, 0)")
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).apply {
            query("SELECT displayName FROM library_source").use { cursor -> cursor.moveToFirst(); assertEquals("Music", cursor.getString(0)) }
            query("SELECT COUNT(*) FROM favorite_tracks").use { cursor -> cursor.moveToFirst(); assertEquals(0, cursor.getInt(0)) }
            query("SELECT COUNT(*) FROM play_history").use { cursor -> cursor.moveToFirst(); assertEquals(0, cursor.getInt(0)) }
            close()
        }
    }

    @Test fun migrationTwoToThreePreservesTracksWithUnknownFingerprints() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL("INSERT INTO tracks(relativePath, documentUri, albumId, title, artist, album, durationMs, trackNumber, discNumber, status, lastSeenScanEpochMillis) VALUES('a.mp3', 'content://a', 'album', 'A', 'Artist', 'Album', 1000, 1, 1, 'PLAYABLE', 1)")
            close()
        }
        helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3).apply {
            query("SELECT title, fileSizeBytes, lastModifiedEpochMillis FROM tracks").use { cursor ->
                cursor.moveToFirst()
                assertEquals("A", cursor.getString(0))
                assertTrue(cursor.isNull(1))
                assertTrue(cursor.isNull(2))
            }
            close()
        }
    }

    private companion object { const val TEST_DB = "migration-listening" }
}
