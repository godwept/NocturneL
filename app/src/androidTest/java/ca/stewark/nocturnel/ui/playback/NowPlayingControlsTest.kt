package ca.stewark.nocturnel.ui.playback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import ca.stewark.nocturnel.playback.PlaybackUiState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NowPlayingControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun repeatButtonShowsTheCurrentModeAndKeepsItsCallback() {
        var state by mutableStateOf(PlaybackUiState(repeatMode = Player.REPEAT_MODE_OFF))
        var repeatClicks = 0
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
                    onRepeat = { repeatClicks++ },
                    onSeek = {},
                )
            }
        }

        compose.onNodeWithText("[ RPT ]").assertIsDisplayed()
        compose.onNodeWithText("[ RPT:A ]").assertDoesNotExist()
        compose.onNodeWithText("[ RPT:1 ]").assertDoesNotExist()
        compose.onNodeWithContentDescription("Repeat off").performClick()
        compose.onNodeWithContentDescription("Repeat all").assertDoesNotExist()
        compose.onNodeWithContentDescription("Repeat one").assertDoesNotExist()
        assertEquals(1, repeatClicks)

        compose.runOnIdle { state = state.copy(repeatMode = Player.REPEAT_MODE_ALL) }
        compose.onNodeWithText("[ RPT ]").assertDoesNotExist()
        compose.onNodeWithText("[ RPT:A ]").assertIsDisplayed()
        compose.onNodeWithText("[ RPT:1 ]").assertDoesNotExist()
        compose.onNodeWithContentDescription("Repeat off").assertDoesNotExist()
        compose.onNodeWithContentDescription("Repeat all").assertIsDisplayed()
        compose.onNodeWithContentDescription("Repeat one").assertDoesNotExist()

        compose.runOnIdle { state = state.copy(repeatMode = Player.REPEAT_MODE_ONE) }
        compose.onNodeWithText("[ RPT ]").assertDoesNotExist()
        compose.onNodeWithText("[ RPT:A ]").assertDoesNotExist()
        compose.onNodeWithText("[ RPT:1 ]").assertIsDisplayed()
        compose.onNodeWithContentDescription("Repeat off").assertDoesNotExist()
        compose.onNodeWithContentDescription("Repeat all").assertDoesNotExist()
        compose.onNodeWithContentDescription("Repeat one").assertIsDisplayed()
    }

    @Test fun currentTrackPlayCountIsPartOfTheAlbumMetadata() {
        compose.setContent {
            NocturneLTheme {
                NowPlayingScreen(
                    state = PlaybackUiState(
                        title = "Carrier",
                        artist = "Signal One",
                        album = "Red Horizon",
                        currentPath = "red/01.flac",
                    ),
                    albumArtwork = null,
                    effectsEnabled = false,
                    onPrevious = {},
                    onToggle = {},
                    onNext = {},
                    onShuffle = {},
                    onRepeat = {},
                    onSeek = {},
                    currentTrackPlayCount = 7,
                )
            }
        }

        compose.onNodeWithText("Red Horizon · 7 PLAY(S)").assertIsDisplayed()
        compose.onNodeWithText("Red Horizon").assertDoesNotExist()
        compose.onNodeWithText("7 PLAY(S)").assertDoesNotExist()
    }

    @Test fun secondaryControlsKeepPlaybackModesLeftAndFavoriteRight() {
        var state by mutableStateOf(
            PlaybackUiState(
                title = "Carrier",
                album = "Red Horizon",
                currentPath = "red/01.flac",
                repeatMode = Player.REPEAT_MODE_ALL,
            ),
        )
        var shuffleClicks = 0
        var repeatClicks = 0
        var favoriteClicks = 0
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.width(412.dp)) {
                    NowPlayingScreen(
                        state = state,
                        albumArtwork = null,
                        effectsEnabled = false,
                        onPrevious = {},
                        onToggle = {},
                        onNext = {},
                        onShuffle = { shuffleClicks++ },
                        onRepeat = { repeatClicks++ },
                        onSeek = {},
                        currentTrackFavorite = false,
                        onToggleCurrentFavorite = { favoriteClicks++ },
                    )
                }
            }
        }

        val shuffle = compose.onNodeWithText("[ SHF ]")
        val repeat = compose.onNodeWithText("[ RPT:A ]")
        val favorite = compose.onNodeWithContentDescription("Add Carrier to favorites")
        val shuffleBounds = shuffle.fetchSemanticsNode().boundsInRoot
        val repeatBounds = repeat.fetchSemanticsNode().boundsInRoot
        val favoriteBounds = compose.onNodeWithText("[ FAV ]").fetchSemanticsNode().boundsInRoot

        assertTrue(abs(shuffleBounds.top - repeatBounds.top) <= 1f)
        assertTrue(abs(repeatBounds.top - favoriteBounds.top) <= 1f)
        assertTrue(shuffleBounds.left < repeatBounds.left)
        assertTrue(repeatBounds.right < favoriteBounds.left)

        shuffle.performClick()
        repeat.performClick()
        favorite.performClick()
        assertEquals(1, shuffleClicks)
        assertEquals(1, repeatClicks)
        assertEquals(1, favoriteClicks)

        compose.runOnIdle { state = state.copy(currentPath = null) }
        compose.onNodeWithContentDescription("Add Carrier to favorites").assertDoesNotExist()
        compose.onNodeWithContentDescription("Remove Carrier from favorites").assertDoesNotExist()
        compose.onNodeWithText("[ FAV ]").assertDoesNotExist()
        compose.onNodeWithText("[ SHF ]").assertIsDisplayed()
        compose.onNodeWithText("[ RPT:A ]").assertIsDisplayed()
    }
}
