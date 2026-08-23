package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import org.junit.Rule
import org.junit.Test

class TerminalVisualizersTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sceneTagsAndEffectsAreStable() {
        compose.setContent {
            NocturneLTheme {
                TerminalVisualizerScene(VisualizerDisplayMode.RADAR, AudioAnalysisFrame.Idle, true, Modifier.size(200.dp))
            }
        }
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        compose.onNodeWithTag("scanlines").assertIsDisplayed()
        compose.onNodeWithText("SIGNAL UNAVAILABLE").assertDoesNotExist()
    }

    @Test fun unavailableSignalIsReadableWithoutEffects() {
        compose.setContent {
            NocturneLTheme {
                TerminalVisualizerScene(VisualizerDisplayMode.BANDS, AudioAnalysisFrame.Unavailable, false, Modifier.size(200.dp))
            }
        }
        compose.onNodeWithText("SIGNAL UNAVAILABLE").assertIsDisplayed()
        compose.onNodeWithTag("scanlines").assertDoesNotExist()
        compose.onNodeWithTag("visualizer-bands").assertIsDisplayed().assertHasNoClickAction()
    }

    @Test fun activeSceneClearsEffectsAndHistoryAcrossLifecycleChanges() {
        compose.mainClock.autoAdvance = false
        var mode by mutableStateOf(VisualizerDisplayMode.RADAR)
        var frame by mutableStateOf(activeFrame(1))
        var effectsEnabled by mutableStateOf(true)
        compose.setContent {
            NocturneLTheme {
                TerminalVisualizerScene(mode, frame, effectsEnabled, Modifier.size(200.dp))
            }
        }
        advanceUi()

        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithTag("scanlines").assertIsDisplayed()

        compose.runOnIdle { effectsEnabled = false }
        compose.mainClock.advanceTimeBy(550)
        advanceUi()
        compose.onNodeWithTag("scanlines").assertDoesNotExist()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed().assertHasNoClickAction()

        compose.runOnIdle {
            effectsEnabled = true
            mode = VisualizerDisplayMode.BANDS
            frame = activeFrame(2)
        }
        advanceUi()
        compose.onNodeWithTag("visualizer-radar").assertDoesNotExist()
        compose.onNodeWithTag("visualizer-bands").assertIsDisplayed().assertHasNoClickAction()

        compose.runOnIdle { frame = AudioAnalysisFrame.Unavailable }
        advanceUi()
        compose.onNodeWithText("SIGNAL UNAVAILABLE").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(550)
        compose.onNodeWithText("SIGNAL UNAVAILABLE").assertIsDisplayed()

        compose.runOnIdle { frame = activeFrame(3) }
        advanceUi()
        compose.onNodeWithText("SIGNAL UNAVAILABLE").assertDoesNotExist()
        compose.onNodeWithTag("visualizer-bands").assertIsDisplayed().assertHasNoClickAction()
    }

    private fun advanceUi() = repeat(2) { compose.mainClock.advanceTimeByFrame() }

    private fun activeFrame(id: Long) = AudioAnalysisFrame(
        waveform = List(128) { 0f },
        bands = List(32) { index -> if (index % 3 == 0) .8f else .2f },
        energy = .5f,
        lowEnergy = .6f,
        midEnergy = .4f,
        highEnergy = .2f,
        transient = .1f,
        frameId = id,
        status = AnalysisStatus.ACTIVE,
    )
}
