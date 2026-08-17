package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
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
    }
}
