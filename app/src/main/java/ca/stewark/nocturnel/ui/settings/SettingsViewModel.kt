package ca.stewark.nocturnel.ui.settings

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import ca.stewark.nocturnel.ui.effects.EffectsPolicy
import ca.stewark.nocturnel.visualizer.VisualizerSyncOffset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TerminalSettingsState(
    val savedEffectsEnabled: Boolean = true,
    val effectiveEffectsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val visualizerSyncOffsetMs: Int = VisualizerSyncOffset.DEFAULT_MS,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TerminalPreferencesRepository(application)
    private val _state = MutableStateFlow(
        resolve(repository.effectsEnabled.value, repository.visualizerSyncOffsetMs.value),
    )
    val state: StateFlow<TerminalSettingsState> = _state.asStateFlow()

    init {
        _state.value = resolve(repository.effectsEnabled.value, repository.visualizerSyncOffsetMs.value)
    }

    fun setEffectsEnabled(enabled: Boolean) {
        repository.setEffectsEnabled(enabled)
        _state.value = resolve(enabled, _state.value.visualizerSyncOffsetMs)
    }

    fun increaseVisualizerSyncOffset() = setVisualizerSyncOffset(
        VisualizerSyncOffset.increase(_state.value.visualizerSyncOffsetMs),
    )

    fun decreaseVisualizerSyncOffset() = setVisualizerSyncOffset(
        VisualizerSyncOffset.decrease(_state.value.visualizerSyncOffsetMs),
    )

    fun resetVisualizerSyncOffset() = setVisualizerSyncOffset(VisualizerSyncOffset.DEFAULT_MS)

    private fun setVisualizerSyncOffset(offsetMs: Int) {
        repository.setVisualizerSyncOffsetMs(offsetMs)
        _state.value = _state.value.copy(visualizerSyncOffsetMs = repository.visualizerSyncOffsetMs.value)
    }

    private fun resolve(saved: Boolean, visualizerSyncOffsetMs: Int): TerminalSettingsState {
        val animationsEnabled = runCatching {
            Settings.Global.getFloat(
                getApplication<Application>().contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) != 0f
        }.getOrDefault(true)
        val policy = EffectsPolicy(saved, animationsEnabled)
        return TerminalSettingsState(
            savedEffectsEnabled = saved,
            effectiveEffectsEnabled = policy.effectiveEffectsEnabled,
            reducedMotion = !animationsEnabled,
            visualizerSyncOffsetMs = VisualizerSyncOffset.clamp(visualizerSyncOffsetMs),
        )
    }
}
