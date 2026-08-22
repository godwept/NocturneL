package ca.stewark.nocturnel.ui.settings

import androidx.test.core.app.ApplicationProvider
import ca.stewark.nocturnel.ui.listening.LibrarySortMode
import ca.stewark.nocturnel.ui.listening.LibraryViewMode
import ca.stewark.nocturnel.ui.theme.FontPreset
import ca.stewark.nocturnel.ui.theme.ColorThemePreset
import ca.stewark.nocturnel.visualizer.VisualizerSyncOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalPreferencesRepositoryTest {
    @Test fun colorThemeDefaultsPersistsAndRestoresStableValue() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-color-theme-test"
        val preferences = context.getSharedPreferences(name, 0)
        preferences.edit().clear().commit()

        val repository = TerminalPreferencesRepository(context, name)
        assertEquals(ColorThemePreset.GREEN_TERMINAL, repository.colorTheme.value)
        repository.setColorTheme(ColorThemePreset.NEON_90S)
        assertEquals(ColorThemePreset.NEON_90S, repository.colorTheme.value)
        assertEquals("90s_neon", preferences.getString("color_theme", null))
        assertEquals(ColorThemePreset.NEON_90S, TerminalPreferencesRepository(context, name).colorTheme.value)
    }

    @Test fun malformedColorThemeFallsBackWithoutChangingOtherPreferences() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-color-theme-malformed-test"
        val preferences = context.getSharedPreferences(name, 0)
        preferences.edit().clear().putBoolean("effects_enabled", false).putString("font_preset", "pixel")
            .putString("color_theme", "unknown").commit()
        assertEquals(ColorThemePreset.GREEN_TERMINAL, TerminalPreferencesRepository(context, name).colorTheme.value)
        assertFalse(preferences.getBoolean("effects_enabled", true))
        assertEquals("pixel", preferences.getString("font_preset", null))

        preferences.edit().putInt("color_theme", 7).commit()
        assertEquals(ColorThemePreset.GREEN_TERMINAL, TerminalPreferencesRepository(context, name).colorTheme.value)
    }

    @Test fun fontPresetDefaultsPersistsAndRestoresStableValue() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-font-preset-test"
        val preferences = context.getSharedPreferences(name, 0)
        preferences.edit().clear().commit()

        val repository = TerminalPreferencesRepository(context, name)
        assertEquals(FontPreset.CLASSIC, repository.fontPreset.value)
        repository.setFontPreset(FontPreset.PIXEL)
        assertEquals(FontPreset.PIXEL, repository.fontPreset.value)
        assertEquals("pixel", preferences.getString("font_preset", null))
        assertEquals(FontPreset.PIXEL, TerminalPreferencesRepository(context, name).fontPreset.value)
    }

    @Test fun malformedFontPresetFallsBackWithoutChangingOtherPreferences() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "terminal-font-preset-malformed-test"
        val preferences = context.getSharedPreferences(name, 0)
        preferences.edit().clear().putBoolean("effects_enabled", false).putString("font_preset", "unknown").commit()
        assertEquals(FontPreset.CLASSIC, TerminalPreferencesRepository(context, name).fontPreset.value)
        assertFalse(preferences.getBoolean("effects_enabled", true))

        preferences.edit().putInt("font_preset", 7).commit()
        assertEquals(FontPreset.CLASSIC, TerminalPreferencesRepository(context, name).fontPreset.value)
        assertFalse(preferences.getBoolean("effects_enabled", true))
    }

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
