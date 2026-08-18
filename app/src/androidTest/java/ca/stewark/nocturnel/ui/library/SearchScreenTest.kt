package ca.stewark.nocturnel.ui.library

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.ui.sampleAlbum
import ca.stewark.nocturnel.ui.sampleTracks
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun trackResultSeparatesPlayAndQueueActions() {
        var played = 0
        var next = 0
        var appended = 0
        compose.setContent {
            NocturneLTheme {
                SearchScreen(
                    sampleTracks, listOf(sampleAlbum), { played++ }, {}, {}, initialQuery = "Carrier",
                    onPlayNext = { next++ }, onAddToQueue = { appended++ },
                )
            }
        }

        compose.onNodeWithContentDescription("Play Carrier next").performClick()
        compose.onNodeWithContentDescription("Add Carrier to queue").performClick()
        assertEquals(0, played)
        assertEquals(1, next)
        assertEquals(1, appended)
        compose.onNodeWithText("Terminal Echo :: Carrier").performClick()
        assertEquals(1, played)
    }
}
