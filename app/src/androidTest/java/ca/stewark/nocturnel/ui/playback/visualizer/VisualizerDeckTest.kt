package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import ca.stewark.nocturnel.visualizer.VisualizerSyncOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VisualizerDeckTest {
    @get:Rule val compose = createComposeRule()

    @Test fun startsOnArtAndCyclesThroughEveryMode() {
        val activity = mutableListOf<Boolean>()
        compose.setContent {
            NocturneLTheme {
                VisualizerDeck(AudioAnalysisFrame.Idle, true, activity::add, Modifier.width(200.dp)) { Text("ARTWORK") }
            }
        }
        compose.onNodeWithTag("visualizer-art").assertIsDisplayed().performClick()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.onNodeWithTag("visualizer-bands").assertIsDisplayed()
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
                    modifier = Modifier.width(240.dp),
                    syncOffsetMs = offsetMs,
                    onDecreaseSyncOffset = {
                        decreases++
                        offsetMs = VisualizerSyncOffset.decrease(offsetMs)
                    },
                    onIncreaseSyncOffset = {
                        increases++
                        offsetMs = VisualizerSyncOffset.increase(offsetMs)
                    },
                    onResetSyncOffset = {
                        resets++
                        offsetMs = VisualizerSyncOffset.DEFAULT_MS
                    },
                ) { Text("ARTWORK") }
            }
        }
        compose.onNodeWithTag("visualizer-sync-controls").assertDoesNotExist()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("visualizer-art").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("visualizer-sync-controls").assertIsDisplayed()
        compose.onNodeWithContentDescription("Decrease visualizer sync offset").assertExists()
        compose.onNodeWithContentDescription("Increase visualizer sync offset").assertExists()
        compose.onNodeWithContentDescription("Reset visualizer sync offset, currently +150 ms").assertExists()
        compose.mainClock.advanceTimeBy(3_001)
        compose.onNodeWithTag("visualizer-sync-reset").assertDoesNotExist()

        compose.onNodeWithTag("visualizer-sync-decrease").assertIsEnabled().performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("VIS SYNC +125 MS", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()

        compose.mainClock.advanceTimeBy(2_000)
        compose.onNodeWithTag("visualizer-sync-increase").assertIsEnabled().performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("VIS SYNC +150 MS", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()

        compose.onNodeWithTag("visualizer-sync-reset").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("VIS SYNC 0 MS", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        assertEquals(1, resets)
        assertEquals(1, decreases)
        assertEquals(1, increases)

        compose.runOnIdle { offsetMs = -2_000 }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("visualizer-sync-decrease").assertIsNotEnabled()
            .performTouchInput { click() }
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        compose.runOnIdle { offsetMs = 2_000 }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("visualizer-sync-increase").assertIsNotEnabled()
            .performTouchInput { click() }
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()

        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("visualizer-sync-controls").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-bands").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("visualizer-sync-controls").assertDoesNotExist()
    }

    @Test fun syncControlsOverlayAnUnmovedSquareVisualizer() {
        compose.setContent {
            NocturneLTheme {
                VisualizerDeck(
                    frame = AudioAnalysisFrame.Idle,
                    effectsEnabled = true,
                    onVisualizerActiveChanged = {},
                    modifier = Modifier.width(240.dp),
                ) { Text("ARTWORK") }
            }
        }

        val artBounds = compose.onNodeWithTag("visualizer-art").fetchSemanticsNode().boundsInRoot
        assertEquals(artBounds.width, artBounds.height, 0.5f)

        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("visualizer-art").performClick()
        compose.mainClock.advanceTimeByFrame()

        val visualizerBounds = compose.onNodeWithTag("visualizer-deck").fetchSemanticsNode().boundsInRoot
        assertEquals(artBounds.left, visualizerBounds.left, 0.5f)
        assertEquals(artBounds.top, visualizerBounds.top, 0.5f)
        assertEquals(artBounds.right, visualizerBounds.right, 0.5f)
        assertEquals(artBounds.bottom, visualizerBounds.bottom, 0.5f)
        assertEquals(visualizerBounds.width, visualizerBounds.height, 0.5f)

        val decreaseBounds = compose.onNodeWithTag("visualizer-sync-decrease").fetchSemanticsNode().boundsInRoot
        val resetBounds = compose.onNodeWithTag("visualizer-sync-reset").fetchSemanticsNode().boundsInRoot
        val increaseBounds = compose.onNodeWithTag("visualizer-sync-increase").fetchSemanticsNode().boundsInRoot
        val modeBounds = compose.onNodeWithTag("visualizer-mode-label").fetchSemanticsNode().boundsInRoot
        assertEquals(visualizerBounds.top, decreaseBounds.top, 0.5f)
        assertEquals(visualizerBounds.top, resetBounds.top, 0.5f)
        assertEquals(visualizerBounds.top, increaseBounds.top, 0.5f)
        assertTrue(decreaseBounds.center.x < visualizerBounds.center.x)
        assertEquals(visualizerBounds.center.x, resetBounds.center.x, 0.5f)
        assertTrue(increaseBounds.center.x > visualizerBounds.center.x)
        assertTrue(modeBounds.center.x > visualizerBounds.center.x)
        assertTrue(modeBounds.center.y > visualizerBounds.center.y)
        assertTrue(modeBounds.right <= visualizerBounds.right)
        assertTrue(modeBounds.bottom <= visualizerBounds.bottom)
    }

    @Test fun syncLabelAppearsForThreeSecondsOnEachVisualizerSelection() {
        compose.setContent {
            NocturneLTheme {
                VisualizerDeck(
                    frame = AudioAnalysisFrame.Idle,
                    effectsEnabled = true,
                    onVisualizerActiveChanged = {},
                    modifier = Modifier.width(240.dp),
                ) { Text("ARTWORK") }
            }
        }

        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("visualizer-art").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("visualizer-sync-reset").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(2_599)
        compose.onNodeWithTag("visualizer-sync-reset").assertExists()
        compose.mainClock.advanceTimeBy(402)
        compose.onNodeWithTag("visualizer-sync-reset").assertDoesNotExist()

        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("visualizer-bands").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-sync-reset").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("visualizer-art").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-sync-controls").assertDoesNotExist()
    }
}
