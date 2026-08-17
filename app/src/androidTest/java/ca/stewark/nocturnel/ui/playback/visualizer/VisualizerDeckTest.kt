package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
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
        compose.onNodeWithTag("visualizer-scope").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.onNodeWithTag("visualizer-art").assertIsDisplayed()
        assertEquals(false, activity.last())
    }
}
