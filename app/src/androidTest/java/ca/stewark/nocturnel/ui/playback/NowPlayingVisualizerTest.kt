package ca.stewark.nocturnel.ui.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.playback.PlaybackUiState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
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
        compose.onNodeWithTag("visualizer-art").assertIsDisplayed().performClick()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
        compose.runOnIdle { state = state.copy(title = "Second", currentPath = "second") }
        compose.onNodeWithText("Second").assertIsDisplayed()
        compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
    }
}
