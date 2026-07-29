package ca.stewark.nocturnel.ui.settings

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import ca.stewark.nocturnel.ui.effects.EffectsPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TerminalSettingsState(
    val savedEffectsEnabled: Boolean = true,
    val effectiveEffectsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TerminalPreferencesRepository(application)
    private val _state = MutableStateFlow(resolve(repository.effectsEnabled.value))
    val state: StateFlow<TerminalSettingsState> = _state.asStateFlow()

    init {
        repository.effectsEnabled.value.let { _state.value = resolve(it) }
    }

    fun setEffectsEnabled(enabled: Boolean) {
        repository.setEffectsEnabled(enabled)
        _state.value = resolve(enabled)
    }

    private fun resolve(saved: Boolean): TerminalSettingsState {
        val animationsEnabled = runCatching {
            Settings.Global.getFloat(
                getApplication<Application>().contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) != 0f
        }.getOrDefault(true)
        val policy = EffectsPolicy(saved, animationsEnabled)
        return TerminalSettingsState(saved, policy.effectiveEffectsEnabled, !animationsEnabled)
    }
}
