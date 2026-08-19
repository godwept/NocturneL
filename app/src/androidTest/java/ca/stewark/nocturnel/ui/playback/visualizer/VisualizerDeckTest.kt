package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VisualizerDeckTest {
    @get:Rule val compose = createComposeRule()

    @Test fun startsOnArtAndCyclesThroughEveryMode() {
        val activity = mutableListOf<Boolean>()
        compose.setContent {
            NocturneLTheme {
                VisualizerDeck(AudioAnalysisFrame.Idle, true, activity::add, Modifier.size(200.dp)) { Text("ARTWORK") }
            }
        }
        compose.onNodeWithTag("visualizer-art").assertIsDisplayed().performClick()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.onNodeWithTag("visualizer-bands").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.onNodeWithTag("visualizer-ring").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.onNodeWithTag("visualizer-art").assertIsDisplayed()
        assertEquals(false, activity.last())
    }

    @Test fun syncControlsAdjustWithoutCyclingAndRespectLimits() {
        var offsetMs by mutableIntStateOf(150)
        var decreases = 0
        var increases = 0
        var resets = 0
        compose.setContent {
            NocturneLTheme {
                VisualizerDeck(
                    frame = AudioAnalysisFrame.Idle,
                    effectsEnabled = true,
                    onVisualizerActiveChanged = {},
                    modifier = Modifier.size(240.dp),
                    syncOffsetMs = offsetMs,
                    onDecreaseSyncOffset = { decreases++ },
                    onIncreaseSyncOffset = { increases++ },
                    onResetSyncOffset = { resets++ },
                ) { Text("ARTWORK") }
            }
        }
        compose.onNodeWithTag("visualizer-sync-controls").assertDoesNotExist()
        compose.onNodeWithTag("visualizer-art").performClick()
        compose.onNodeWithTag("visualizer-sync-controls").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-sync-reset").assertIsDisplayed().performClick()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-sync-decrease").assertIsEnabled().performClick()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-sync-increase").assertIsEnabled().performClick()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        assertEquals(1, resets)
        assertEquals(1, decreases)
        assertEquals(1, increases)

        compose.runOnIdle { offsetMs = -500 }
        compose.onNodeWithTag("visualizer-sync-decrease").assertIsNotEnabled()
        compose.runOnIdle { offsetMs = 1_000 }
        compose.onNodeWithTag("visualizer-sync-increase").assertIsNotEnabled()

        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.onNodeWithTag("visualizer-sync-controls").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-ring").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.onNodeWithTag("visualizer-sync-controls").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.onNodeWithTag("visualizer-sync-controls").assertDoesNotExist()
    }
}
