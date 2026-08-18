package ca.stewark.nocturnel.ui.playback

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.playback.PlaybackQueueItem
import ca.stewark.nocturnel.playback.PlaybackUiState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NowPlayingQueueTest {
    @get:Rule val compose = createComposeRule()

    @Test fun queueSummaryOpensEditorWithoutListingEveryTrack() {
        var opened = 0
        val state = PlaybackUiState(upNext = listOf(
            PlaybackQueueItem("one", "one.flac", "Hidden One", "Artist"),
            PlaybackQueueItem("two", "two.flac", "Hidden Two", "Artist"),
        ))
        compose.setContent {
            NocturneLTheme { NowPlayingScreen(state, null, false, {}, {}, {}, {}, {}, {}, onOpenQueue = { opened++ }) }
        }

        compose.onNodeWithText("2 TRACK(S) UPCOMING").assertIsDisplayed()
        compose.onNodeWithText("Hidden One").assertDoesNotExist()
        compose.onNodeWithText("[ QUEUE ]").performClick()
        assertEquals(1, opened)
    }
}
