package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
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
import kotlin.math.abs

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

    @Test fun audioModesUseTheFullViewport() {
        var mode by mutableStateOf(VisualizerDisplayMode.RADAR)
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.size(240.dp, 480.dp)) {
                    FullScreenVisualizer(mode, AudioAnalysisFrame.Idle, false, {}, {}, {})
                }
            }
        }

        fun assertFills(tag: String) {
            val root = compose.onNodeWithTag("full-screen-visualizer").fetchSemanticsNode().boundsInRoot
            val scene = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertEquals(root.width, scene.width, 1f)
            assertEquals(root.height, scene.height, 1f)
        }

        assertFills("visualizer-radar")
        compose.runOnIdle { mode = VisualizerDisplayMode.GRID }
        assertFills("visualizer-grid")
        compose.runOnIdle { mode = VisualizerDisplayMode.BANDS }
        assertFills("visualizer-bands")
    }

    @Test fun gridSceneOccupiesTheTallFullScreenViewport() {
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.size(240.dp, 480.dp)) {
                    FullScreenVisualizer(
                        VisualizerDisplayMode.GRID,
                        AudioAnalysisFrame.Idle,
                        false,
                        {},
                        {},
                        {},
                    )
                }
            }
        }

        val root = compose.onNodeWithTag("full-screen-visualizer").fetchSemanticsNode().boundsInRoot
        val grid = compose.onNodeWithTag("visualizer-grid").fetchSemanticsNode().boundsInRoot
        assertEquals(root.width, grid.width, 1f)
        assertEquals(root.height, grid.height, 1f)
        assertTrue(grid.height > grid.width)
    }

    @Test fun legacyMarginGlowIsAbsent() {
        var mode by mutableStateOf(VisualizerDisplayMode.RADAR)
        val frame = AudioAnalysisFrame.Idle.copy(status = AnalysisStatus.ACTIVE, transient = 1f)
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.size(240.dp, 480.dp)) {
                    FullScreenVisualizer(mode, frame, true, {}, {}, {})
                }
            }
        }

        compose.onNodeWithTag("full-screen-glow").assertDoesNotExist()
        compose.runOnIdle { mode = VisualizerDisplayMode.GRID }
        compose.onNodeWithTag("full-screen-glow").assertDoesNotExist()
        compose.runOnIdle { mode = VisualizerDisplayMode.BANDS }
        compose.onNodeWithTag("full-screen-glow").assertDoesNotExist()
    }

    @Test fun activeExpandedRadarDrawsBeyondTheCircularCoreOnlyWhenEffectsAreEnabled() {
        var effects by mutableStateOf(true)
        val frame = AudioAnalysisFrame.Idle.copy(
            status = AnalysisStatus.ACTIVE,
            transient = .8f,
            frameId = 0L,
        )
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.size(240.dp, 480.dp)) {
                    FullScreenVisualizer(
                        VisualizerDisplayMode.RADAR,
                        frame,
                        effects,
                        {},
                        {},
                        {},
                    )
                }
            }
        }

        fun difference(first: Color, second: Color): Float =
            abs(first.red - second.red) +
                abs(first.green - second.green) +
                abs(first.blue - second.blue)

        val enabledImage = compose.onNodeWithTag("visualizer-radar").captureToImage()
        val enabled = enabledImage.toPixelMap()
        val enabledY = enabledImage.height / 8
        val enabledBeam = enabled[enabledImage.width / 2, enabledY]
        val enabledBackground = enabled[enabledImage.width / 8, enabledY]
        assertTrue(difference(enabledBeam, enabledBackground) > .01f)

        compose.runOnIdle { effects = false }
        val disabledImage = compose.onNodeWithTag("visualizer-radar").captureToImage()
        val disabled = disabledImage.toPixelMap()
        val disabledY = disabledImage.height / 8
        val disabledCenter = disabled[disabledImage.width / 2, disabledY]
        val disabledBackground = disabled[disabledImage.width / 8, disabledY]
        assertTrue(difference(disabledCenter, disabledBackground) < .01f)
    }
}
