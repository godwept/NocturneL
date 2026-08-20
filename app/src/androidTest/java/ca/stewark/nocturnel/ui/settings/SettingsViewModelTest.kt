package ca.stewark.nocturnel.ui.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import ca.stewark.nocturnel.visualizer.VisualizerSyncOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {
    @Test fun adjustsResetsClampsAndRestoresVisualizerSyncOffset() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val preferences = application.getSharedPreferences("terminal_preferences", 0)
        preferences.edit().clear().commit()
        try {
            val viewModel = SettingsViewModel(application)
            assertEquals(VisualizerSyncOffset.DEFAULT_MS, viewModel.state.value.visualizerSyncOffsetMs)
            viewModel.increaseVisualizerSyncOffset()
            assertEquals(25, viewModel.state.value.visualizerSyncOffsetMs)
            viewModel.decreaseVisualizerSyncOffset()
            assertEquals(0, viewModel.state.value.visualizerSyncOffsetMs)
            repeat(VisualizerSyncOffset.MAX_MS / VisualizerSyncOffset.STEP_MS + 1) {
                viewModel.increaseVisualizerSyncOffset()
            }
            assertEquals(VisualizerSyncOffset.MAX_MS, viewModel.state.value.visualizerSyncOffsetMs)
            repeat(
                (VisualizerSyncOffset.MAX_MS - VisualizerSyncOffset.MIN_MS) /
                    VisualizerSyncOffset.STEP_MS + 1,
            ) {
                viewModel.decreaseVisualizerSyncOffset()
            }
            assertEquals(VisualizerSyncOffset.MIN_MS, viewModel.state.value.visualizerSyncOffsetMs)
            viewModel.resetVisualizerSyncOffset()
            assertEquals(0, viewModel.state.value.visualizerSyncOffsetMs)
            viewModel.increaseVisualizerSyncOffset()
            assertEquals(25, SettingsViewModel(application).state.value.visualizerSyncOffsetMs)
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
