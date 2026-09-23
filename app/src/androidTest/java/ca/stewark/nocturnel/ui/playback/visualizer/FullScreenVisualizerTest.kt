package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FullScreenVisualizerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun exitStartsHiddenAndTapRevealsIt() {
        var exits = 0
        compose.setContent {
            NocturneLTheme {
                FullScreenVisualizer(
                    mode = VisualizerDisplayMode.RADAR,
                    frame = AudioAnalysisFrame.Idle,
                    effectsEnabled = false,
                    onExit = { exits++ },
                    onSwipeLeft = {},
                    onSwipeRight = {},
                )
            }
        }
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        compose.onNodeWithTag("full-screen-exit").assertDoesNotExist()
        compose.onNodeWithTag("full-screen-touch-surface").performClick()
        compose.onNodeWithTag("full-screen-exit").assertIsDisplayed().performClick()
        assertEquals(1, exits)
    }

    @Test fun exitHidesThreeSecondsAfterTheLatestTap() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            NocturneLTheme {
                FullScreenVisualizer(VisualizerDisplayMode.RADAR, AudioAnalysisFrame.Idle, false, {}, {}, {})
            }
        }
        compose.onNodeWithTag("full-screen-touch-surface").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(2_900)
        compose.onNodeWithTag("full-screen-exit").assertIsDisplayed()
        compose.onNodeWithTag("full-screen-touch-surface").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithTag("full-screen-exit").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(2_900)
        compose.onNodeWithTag("full-screen-exit").assertDoesNotExist()
    }

    @Test fun swipesChangeModesWithoutShowingExit() {
        var mode by mutableStateOf(VisualizerDisplayMode.RADAR)
        compose.setContent {
            NocturneLTheme {
                FullScreenVisualizer(
                    mode = mode,
                    frame = AudioAnalysisFrame.Idle,
                    effectsEnabled = false,
                    onExit = {},
                    onSwipeLeft = { mode = VisualizerSessionState(mode, true).swipeLeft().mode },
                    onSwipeRight = { mode = VisualizerSessionState(mode, true).swipeRight().mode },
                )
            }
        }
        compose.onNodeWithTag("full-screen-touch-surface").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("visualizer-bands").assertIsDisplayed()
        compose.onNodeWithTag("full-screen-exit").assertDoesNotExist()
        compose.onNodeWithTag("full-screen-touch-surface").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("visualizer-grid").assertIsDisplayed()
        compose.onNodeWithTag("full-screen-touch-surface").performTouchInput { swipeRight() }
        compose.onNodeWithTag("visualizer-bands").assertIsDisplayed()
    }

    @Test fun squareModesAreCenteredAndBandsFillTheViewport() {
        var mode by mutableStateOf(VisualizerDisplayMode.RADAR)
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.size(240.dp, 480.dp)) {
                    FullScreenVisualizer(mode, AudioAnalysisFrame.Idle, false, {}, {}, {})
                }
            }
        }
        val root = compose.onNodeWithTag("full-screen-visualizer").fetchSemanticsNode().boundsInRoot
        val radar = compose.onNodeWithTag("visualizer-radar").fetchSemanticsNode().boundsInRoot
        assertEquals(radar.width, radar.height, 1f)
        assertEquals(root.center.x, radar.center.x, 1f)
        assertEquals(root.center.y, radar.center.y, 1f)
        compose.runOnIdle { mode = VisualizerDisplayMode.GRID }
        val grid = compose.onNodeWithTag("visualizer-grid").fetchSemanticsNode().boundsInRoot
        assertEquals(grid.width, grid.height, 1f)
        assertEquals(root.center.y, grid.center.y, 1f)
        compose.runOnIdle { mode = VisualizerDisplayMode.BANDS }
        val bands = compose.onNodeWithTag("visualizer-bands").fetchSemanticsNode().boundsInRoot
        assertTrue(bands.height > bands.width)
        assertEquals(root.height, bands.height, 1f)
    }

    @Test fun glowAppearsOnlyForActiveSquareModesWithEffects() {
        var mode by mutableStateOf(VisualizerDisplayMode.RADAR)
        var frame by mutableStateOf(AudioAnalysisFrame.Idle.copy(status = AnalysisStatus.ACTIVE, transient = 1f))
        var effects by mutableStateOf(true)
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.size(240.dp, 480.dp)) {
                    FullScreenVisualizer(mode, frame, effects, {}, {}, {})
                }
            }
        }
        compose.onNodeWithTag("full-screen-glow").assertIsDisplayed()
        compose.runOnIdle { mode = VisualizerDisplayMode.GRID }
        compose.onNodeWithTag("full-screen-glow").assertIsDisplayed()
        compose.runOnIdle { mode = VisualizerDisplayMode.BANDS }
        compose.onNodeWithTag("full-screen-glow").assertDoesNotExist()
        compose.runOnIdle { mode = VisualizerDisplayMode.RADAR; frame = AudioAnalysisFrame.Unavailable }
        compose.onNodeWithTag("full-screen-glow").assertDoesNotExist()
        compose.runOnIdle { frame = AudioAnalysisFrame.Idle.copy(status = AnalysisStatus.ACTIVE, transient = 1f); effects = false }
        compose.onNodeWithTag("full-screen-glow").assertDoesNotExist()
    }
}
