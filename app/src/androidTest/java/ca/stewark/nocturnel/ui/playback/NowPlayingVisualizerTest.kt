package ca.stewark.nocturnel.ui.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.playback.PlaybackUiState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NowPlayingVisualizerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun startsOnArtworkAndKeepsModeAcrossTrackChanges() {
        var state by mutableStateOf(PlaybackUiState(title = "First", currentPath = "first", playing = true))
        compose.setContent {
            NocturneLTheme {
                NowPlayingScreen(
                    state = state,
                    albumArtwork = null,
                    effectsEnabled = false,
                    onPrevious = {},
                    onToggle = {},
                    onNext = {},
                    onShuffle = {},
                    onRepeat = {},
                    onSeek = {},
                )
            }
        }
        compose.onNodeWithText("[ NOW PLAYING ]").assertDoesNotExist()
        compose.onNodeWithTag("visualizer-art").assertIsDisplayed().performClick()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(title = "Second", currentPath = "second") }
        compose.onNodeWithText("Second").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
    }

    @Test fun exposesSharedSyncControlsOnlyForVisualizerModes() {
        var offsetMs by mutableIntStateOf(75)
        var decreases = 0
        var increases = 0
        var resets = 0
        compose.setContent {
            NocturneLTheme {
                NowPlayingScreen(
                    state = PlaybackUiState(title = "Track", currentPath = "track", playing = true),
                    albumArtwork = null,
                    effectsEnabled = false,
                    onPrevious = {},
                    onToggle = {},
                    onNext = {},
                    onShuffle = {},
                    onRepeat = {},
                    onSeek = {},
                    visualizerSyncOffsetMs = offsetMs,
                    onDecreaseVisualizerSyncOffset = { decreases++; offsetMs -= 25 },
                    onIncreaseVisualizerSyncOffset = { increases++; offsetMs += 25 },
                    onResetVisualizerSyncOffset = { resets++; offsetMs = 0 },
                )
            }
        }
        compose.onNodeWithTag("visualizer-sync-controls").assertDoesNotExist()
        compose.onNodeWithTag("visualizer-art").performClick()
        compose.onNodeWithText("VIS SYNC +75 MS", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("visualizer-sync-decrease").performClick()
        compose.onNodeWithTag("visualizer-sync-increase").performClick()
        compose.onNodeWithTag("visualizer-sync-reset").performClick()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        assertEquals(1, decreases)
        assertEquals(1, increases)
        assertEquals(1, resets)
    }
}
