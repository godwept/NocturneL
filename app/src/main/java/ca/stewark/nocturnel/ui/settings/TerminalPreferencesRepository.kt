package ca.stewark.nocturnel.ui.settings

import android.content.Context
import android.content.SharedPreferences
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

    fun setEffectsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(EFFECTS_ENABLED, enabled).apply()
        _effectsEnabled.value = enabled
    }

    private companion object {
        const val EFFECTS_ENABLED = "effects_enabled"
    }
}
