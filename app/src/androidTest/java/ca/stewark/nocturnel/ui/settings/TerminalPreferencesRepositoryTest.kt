package ca.stewark.nocturnel.ui.settings

import androidx.test.core.app.ApplicationProvider
import ca.stewark.nocturnel.ui.listening.LibrarySortMode
import ca.stewark.nocturnel.ui.listening.LibraryViewMode
import ca.stewark.nocturnel.visualizer.VisualizerSyncOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalPreferencesRepositoryTest {
    @Test fun libraryViewModeDefaultsPersistsAndRestores() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-library-view-test"
        val preferences = context.getSharedPreferences(name, 0)
        preferences.edit().clear().commit()

        val repository = TerminalPreferencesRepository(context, name)
        assertEquals(LibraryViewMode.GRID, repository.libraryViewMode.value)
        repository.setLibraryViewMode(LibraryViewMode.FLOW)
        assertEquals(LibraryViewMode.FLOW, repository.libraryViewMode.value)
        assertEquals(LibraryViewMode.FLOW, TerminalPreferencesRepository(context, name).libraryViewMode.value)
    }

    @Test fun malformedLibraryViewModeFallsBackToGrid() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-library-view-malformed-test"
        val preferences = context.getSharedPreferences(name, 0)
        preferences.edit().clear().putString("library_view_mode", "UNKNOWN").commit()
        assertEquals(LibraryViewMode.GRID, TerminalPreferencesRepository(context, name).libraryViewMode.value)

        preferences.edit().clear().putInt("library_view_mode", 7).commit()
        assertEquals(LibraryViewMode.GRID, TerminalPreferencesRepository(context, name).libraryViewMode.value)
    }

    @Test fun librarySortModeDefaultsPersistsAndRestores() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-library-sort-test"
        val preferences = context.getSharedPreferences(name, 0)
        preferences.edit().clear().commit()

        assertEquals(LibrarySortMode.ARTIST, TerminalPreferencesRepository(context, name).librarySortMode.value)
        TerminalPreferencesRepository(context, name).setLibrarySortMode(LibrarySortMode.YEAR)
        assertEquals(LibrarySortMode.YEAR, TerminalPreferencesRepository(context, name).librarySortMode.value)
    }

    @Test fun malformedLibrarySortModeFallsBackToArtist() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-library-sort-malformed-test"
        val preferences = context.getSharedPreferences(name, 0)
        preferences.edit().clear().putString("library_sort_mode", "UNKNOWN").commit()
        assertEquals(LibrarySortMode.ARTIST, TerminalPreferencesRepository(context, name).librarySortMode.value)

        preferences.edit().clear().putInt("library_sort_mode", 7).commit()
        assertEquals(LibrarySortMode.ARTIST, TerminalPreferencesRepository(context, name).librarySortMode.value)
    }

    @Test fun preferenceDefaultsOnAndPersistsOff() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-preferences-test"
        context.getSharedPreferences(name, 0).edit().clear().commit()
        assertTrue(TerminalPreferencesRepository(context, name).effectsEnabled.value)
        TerminalPreferencesRepository(context, name).setEffectsEnabled(false)
        assertFalse(TerminalPreferencesRepository(context, name).effectsEnabled.value)
    }

    @Test fun visualizerSyncOffsetDefaultsPersistsAndClamps() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-sync-preferences-test"
        val preferences = context.getSharedPreferences(name, 0)
        preferences.edit().clear().commit()

        assertEquals(VisualizerSyncOffset.DEFAULT_MS, TerminalPreferencesRepository(context, name).visualizerSyncOffsetMs.value)
        TerminalPreferencesRepository(context, name).setVisualizerSyncOffsetMs(175)
        assertEquals(175, TerminalPreferencesRepository(context, name).visualizerSyncOffsetMs.value)

        TerminalPreferencesRepository(context, name).setVisualizerSyncOffsetMs(-2_000)
        assertEquals(-2_000, TerminalPreferencesRepository(context, name).visualizerSyncOffsetMs.value)
        TerminalPreferencesRepository(context, name).setVisualizerSyncOffsetMs(2_000)
        assertEquals(2_000, TerminalPreferencesRepository(context, name).visualizerSyncOffsetMs.value)

        preferences.edit().putInt("visualizer_sync_offset_ms", -9_999).commit()
        assertEquals(VisualizerSyncOffset.MIN_MS, TerminalPreferencesRepository(context, name).visualizerSyncOffsetMs.value)
        preferences.edit().putInt("visualizer_sync_offset_ms", 9_999).commit()
        assertEquals(VisualizerSyncOffset.MAX_MS, TerminalPreferencesRepository(context, name).visualizerSyncOffsetMs.value)

        val repository = TerminalPreferencesRepository(context, name)
        repository.setVisualizerSyncOffsetMs(9_999)
        assertEquals(VisualizerSyncOffset.MAX_MS, repository.visualizerSyncOffsetMs.value)
        assertEquals(VisualizerSyncOffset.MAX_MS, preferences.getInt("visualizer_sync_offset_ms", 0))
    }

    @Test fun wrongTypedVisualizerSyncOffsetFallsBackToDefault() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-sync-malformed-test"
        context.getSharedPreferences(name, 0).edit()
            .clear()
            .putString("visualizer_sync_offset_ms", "later")
            .commit()

        assertEquals(VisualizerSyncOffset.DEFAULT_MS, TerminalPreferencesRepository(context, name).visualizerSyncOffsetMs.value)
    }
}
