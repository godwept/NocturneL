package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VisualizerPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun expansionReplacesNormalContentAndExitKeepsTheLastMode() {
        var session by mutableStateOf(VisualizerSessionState())
        compose.setContent {
            NocturneLTheme {
                VisualizerPresentation(
                    session = session,
                    frame = AudioAnalysisFrame.Idle,
                    effectsEnabled = false,
                    nowVisible = true,
                    onSessionChange = { session = it },
                    onVisualizerActiveChanged = {},
                ) { Text("NOW CONTENT") }
            }
        }
        compose.onNodeWithText("NOW CONTENT").assertIsDisplayed()
        compose.runOnIdle { session = session.open() }
        compose.onNodeWithText("NOW CONTENT").assertDoesNotExist()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        compose.runOnIdle { session = session.swipeLeft().swipeLeft() }
        compose.onNodeWithTag("visualizer-grid").assertIsDisplayed()
        compose.onNodeWithTag("full-screen-touch-surface").performClick()
        compose.onNodeWithTag("full-screen-exit").performClick()
        compose.onNodeWithText("NOW CONTENT").assertIsDisplayed()
        assertEquals(VisualizerDisplayMode.GRID, session.mode)
    }

    @Test fun analysisRemainsActiveDuringExpansionAndExit() {
        var session by mutableStateOf(VisualizerSessionState(VisualizerDisplayMode.RADAR))
        val changes = mutableListOf<Boolean>()
        compose.setContent {
            NocturneLTheme {
                VisualizerPresentation(
                    session = session,
                    frame = AudioAnalysisFrame.Idle,
                    effectsEnabled = false,
                    nowVisible = true,
                    onSessionChange = { session = it },
                    onVisualizerActiveChanged = changes::add,
                ) { Text("NOW CONTENT") }
            }
        }
        compose.waitForIdle()
        assertEquals(listOf(true), changes)
        compose.runOnIdle { session = session.open() }
        compose.runOnIdle { session = session.close() }
        assertEquals(listOf(true), changes)
        compose.runOnIdle { session = session.copy(mode = VisualizerDisplayMode.ART) }
        assertEquals(listOf(true, false), changes)
    }

    @Test fun systemBackClosesTheExpandedView() {
        var session by mutableStateOf(VisualizerSessionState().open())
        compose.setContent {
            NocturneLTheme {
                VisualizerPresentation(session, AudioAnalysisFrame.Idle, false, true, { session = it }, {}) {
                    Text("NOW CONTENT")
                }
            }
        }
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithText("NOW CONTENT").assertIsDisplayed()
    }
}
