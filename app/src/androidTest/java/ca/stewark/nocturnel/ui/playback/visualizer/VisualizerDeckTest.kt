package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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
        compose.onNodeWithTag("visualizer-grid").assertIsDisplayed()
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
        advanceUi()
        compose.onNodeWithTag("visualizer-sync-controls").assertIsDisplayed()
        compose.onNodeWithContentDescription("Decrease visualizer sync offset").assertExists()
        compose.onNodeWithContentDescription("Increase visualizer sync offset").assertExists()
        compose.onNodeWithContentDescription("Reset visualizer sync offset, currently +150 ms").assertExists()
        compose.mainClock.advanceTimeBy(3_001)
        advanceUi()
        compose.onNodeWithTag("visualizer-sync-reset").assertDoesNotExist()

        compose.onNodeWithTag("visualizer-sync-decrease").assertIsEnabled().performClick()
        advanceUi()
        compose.onNodeWithText("VIS SYNC +125 MS", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()

        compose.mainClock.advanceTimeBy(2_000)
        compose.onNodeWithTag("visualizer-sync-increase").assertIsEnabled().performClick()
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithText("VIS SYNC +150 MS", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()

        compose.onNodeWithTag("visualizer-sync-reset").performClick()
        advanceUi()
        compose.onNodeWithText("VIS SYNC 0 MS", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        assertEquals(1, resets)
        assertEquals(1, decreases)
        assertEquals(1, increases)

        compose.runOnIdle { offsetMs = -2_000 }
        advanceUi()
        compose.onNodeWithTag("visualizer-sync-decrease").assertIsNotEnabled()
            .performTouchInput { click() }
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        compose.runOnIdle { offsetMs = 2_000 }
        advanceUi()
        compose.onNodeWithTag("visualizer-sync-increase").assertIsNotEnabled()
            .performTouchInput { click() }
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()

        compose.onNodeWithTag("visualizer-deck").performClick()
        advanceUi()
        compose.onNodeWithTag("visualizer-sync-controls").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-bands").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        advanceUi()
        compose.onNodeWithTag("visualizer-grid").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-sync-controls").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        advanceUi()
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
        advanceUi()

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
        advanceUi()
        compose.onNodeWithTag("visualizer-sync-reset").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(2_599)
        compose.onNodeWithTag("visualizer-sync-reset").assertExists()
        compose.mainClock.advanceTimeBy(402)
        compose.onNodeWithTag("visualizer-sync-reset").assertDoesNotExist()

        compose.onNodeWithTag("visualizer-deck").performClick()
        advanceUi()
        compose.onNodeWithTag("visualizer-bands").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-sync-reset").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        advanceUi()
        compose.onNodeWithTag("visualizer-grid").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-sync-reset").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-deck").performClick()
        advanceUi()
        compose.onNodeWithTag("visualizer-art").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-sync-controls").assertDoesNotExist()
    }

    @Test fun syncTouchAdjustsOnDownWithoutASecondReleaseAdjustment() {
        var offsetMs by mutableIntStateOf(150)
        var increases = 0
        compose.setContent {
            NocturneLTheme {
                VisualizerDeck(
                    frame = AudioAnalysisFrame.Idle,
                    effectsEnabled = true,
                    onVisualizerActiveChanged = {},
                    modifier = Modifier.width(240.dp),
                    syncOffsetMs = offsetMs,
                    onIncreaseSyncOffset = {
                        increases++
                        offsetMs = VisualizerSyncOffset.increase(offsetMs)
                    },
                ) { Text("ARTWORK") }
            }
        }

        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("visualizer-art").performClick()
        advanceUi()
        val increase = compose.onNodeWithTag("visualizer-sync-increase")
        increase.performTouchInput { down(center) }
        compose.runOnIdle {
            assertEquals(175, offsetMs)
            assertEquals(1, increases)
        }

        increase.performTouchInput { up() }
        compose.runOnIdle {
            assertEquals(175, offsetMs)
            assertEquals(1, increases)
        }
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()

        increase.performClick()
        advanceUi()
        assertEquals(200, offsetMs)
        assertEquals(2, increases)
    }

    @Test fun syncHoldRepeatsAfterDelayAndAccelerates() {
        var offsetMs by mutableIntStateOf(0)
        var increases = 0
        compose.setContent {
            NocturneLTheme {
                VisualizerDeck(
                    frame = AudioAnalysisFrame.Idle,
                    effectsEnabled = true,
                    onVisualizerActiveChanged = {},
                    modifier = Modifier.width(240.dp),
                    syncOffsetMs = offsetMs,
                    onIncreaseSyncOffset = {
                        increases++
                        offsetMs = VisualizerSyncOffset.increase(offsetMs)
                    },
                ) { Text("ARTWORK") }
            }
        }

        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("visualizer-art").performClick()
        advanceUi()
        val increase = compose.onNodeWithTag("visualizer-sync-increase")
        increase.performTouchInput { down(center) }
        compose.runOnIdle { assertEquals(1, increases) }

        compose.mainClock.advanceTimeBy(399, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals(1, increases) }
        compose.mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals(2, increases) }
        compose.mainClock.advanceTimeBy(100, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals(3, increases) }

        compose.mainClock.advanceTimeBy(999, ignoreFrameDuration = true)
        val beforeAccelerationThreshold = increases
        compose.mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals(beforeAccelerationThreshold + 1, increases) }
        val atAccelerationThreshold = increases
        compose.mainClock.advanceTimeBy(49, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals(atAccelerationThreshold, increases) }
        compose.mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals(atAccelerationThreshold + 1, increases) }

        increase.performTouchInput { up() }
        val atRelease = increases
        compose.mainClock.advanceTimeBy(500, ignoreFrameDuration = true)
        compose.runOnIdle {
            assertEquals(atRelease, increases)
            assertEquals(increases * VisualizerSyncOffset.STEP_MS, offsetMs)
        }
    }

    @Test fun syncHoldStopsOnCancellationMovementAndLimit() {
        var offsetMs by mutableIntStateOf(0)
        var increases = 0
        compose.setContent {
            NocturneLTheme {
                VisualizerDeck(
                    frame = AudioAnalysisFrame.Idle,
                    effectsEnabled = true,
                    onVisualizerActiveChanged = {},
                    modifier = Modifier.width(240.dp),
                    syncOffsetMs = offsetMs,
                    onIncreaseSyncOffset = {
                        increases++
                        offsetMs = VisualizerSyncOffset.increase(offsetMs)
                    },
                ) { Text("ARTWORK") }
            }
        }

        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("visualizer-art").performClick()
        advanceUi()
        val increase = compose.onNodeWithTag("visualizer-sync-increase")

        increase.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(400, ignoreFrameDuration = true)
        increase.performTouchInput { cancel() }
        val afterCancel = increases
        compose.mainClock.advanceTimeBy(500, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals(afterCancel, increases) }

        increase.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(400, ignoreFrameDuration = true)
        increase.performTouchInput { moveTo(Offset(-1f, center.y)) }
        val afterMoveOutside = increases
        compose.mainClock.advanceTimeBy(500, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals(afterMoveOutside, increases) }
        increase.performTouchInput { up() }

        compose.runOnIdle { offsetMs = VisualizerSyncOffset.MAX_MS - 50 }
        advanceUi()
        increase.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(1_000, ignoreFrameDuration = true)
        assertEquals(VisualizerSyncOffset.MAX_MS, offsetMs)
        val atLimit = increases
        compose.mainClock.advanceTimeBy(500, ignoreFrameDuration = true)
        compose.runOnIdle { assertEquals(atLimit, increases) }
        compose.onNodeWithTag("visualizer-sync-increase").assertIsNotEnabled()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
    }

    @Test fun newSyncPressCancelsTheOppositeActiveHold() {
        var offsetMs by mutableIntStateOf(0)
        var decreases = 0
        var increases = 0
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
                ) { Text("ARTWORK") }
            }
        }

        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("visualizer-art").performClick()
        advanceUi()
        val decreaseCenter = compose.onNodeWithTag("visualizer-sync-decrease")
            .fetchSemanticsNode().boundsInRoot.center
        val increaseCenter = compose.onNodeWithTag("visualizer-sync-increase")
            .fetchSemanticsNode().boundsInRoot.center
        val root = compose.onRoot()

        root.performTouchInput { down(0, decreaseCenter) }
        compose.mainClock.advanceTimeBy(400)
        advanceUi()
        root.performTouchInput { down(1, increaseCenter) }
        advanceUi()
        val decreasesAfterOppositePress = decreases
        compose.mainClock.advanceTimeBy(500)
        advanceUi()
        assertEquals(decreasesAfterOppositePress, decreases)
        assertTrue(increases > 1)

        root.performTouchInput { up(1); up(0) }
        val increasesAfterRelease = increases
        compose.mainClock.advanceTimeBy(500)
        advanceUi()
        assertEquals(increasesAfterRelease, increases)
    }

    private fun advanceUi() = repeat(2) { compose.mainClock.advanceTimeByFrame() }
}
