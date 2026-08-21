package ca.stewark.nocturnel.ui.settings

import android.content.Context
import android.content.SharedPreferences
import ca.stewark.nocturnel.ui.listening.LibrarySortMode
import ca.stewark.nocturnel.visualizer.VisualizerSyncOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TerminalPreferencesRepository(
    context: Context,
    preferencesName: String = "terminal_preferences",
) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val _effectsEnabled = MutableStateFlow(preferences.getBoolean(EFFECTS_ENABLED, true))
    val effectsEnabled: StateFlow<Boolean> = _effectsEnabled.asStateFlow()
    private val _librarySortMode = MutableStateFlow(
        LibrarySortMode.fromPersisted(
            runCatching { preferences.getString(LIBRARY_SORT_MODE, null) }.getOrNull(),
        ),
    )
    val librarySortMode: StateFlow<LibrarySortMode> = _librarySortMode.asStateFlow()
    private val _visualizerSyncOffsetMs = MutableStateFlow(
        runCatching { preferences.getInt(VISUALIZER_SYNC_OFFSET_MS, VisualizerSyncOffset.DEFAULT_MS) }
            .getOrDefault(VisualizerSyncOffset.DEFAULT_MS)
            .let(VisualizerSyncOffset::clamp),
    )
    val visualizerSyncOffsetMs: StateFlow<Int> = _visualizerSyncOffsetMs.asStateFlow()

    fun setEffectsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(EFFECTS_ENABLED, enabled).apply()
        _effectsEnabled.value = enabled
    }

    fun setLibrarySortMode(mode: LibrarySortMode) {
        preferences.edit().putString(LIBRARY_SORT_MODE, mode.name).apply()
        _librarySortMode.value = mode
    }

    fun setVisualizerSyncOffsetMs(offsetMs: Int) {
        val clamped = VisualizerSyncOffset.clamp(offsetMs)
        runCatching { preferences.edit().putInt(VISUALIZER_SYNC_OFFSET_MS, clamped).apply() }
        _visualizerSyncOffsetMs.value = clamped
    }

    private companion object {
        const val EFFECTS_ENABLED = "effects_enabled"
        const val LIBRARY_SORT_MODE = "library_sort_mode"
        const val VISUALIZER_SYNC_OFFSET_MS = "visualizer_sync_offset_ms"
    }
}
