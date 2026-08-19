package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.ui.sampleAlbum
import ca.stewark.nocturnel.ui.sampleTracks
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun trackResultSeparatesPlayAndQueueActions() {
        var played = 0
        var appended = 0
        compose.setContent {
            NocturneLTheme {
                SearchScreen(
                    sampleTracks, listOf(sampleAlbum), { played++ }, {}, {}, initialQuery = "Carrier",
                    onAddToQueue = { appended++ },
                )
            }
        }

        compose.onNodeWithContentDescription("Play Carrier next").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add Carrier to queue").performClick()
        assertEquals(0, played)
        assertEquals(1, appended)
        compose.onNodeWithText("Terminal Echo :: Carrier", substring = true).performClick()
        assertEquals(1, played)
        assertEquals(1, appended)
    }

    @Test fun longTrackResultIsOneEllipsizedSemanticLine() {
        val longTitle = "Carrier Across The Endless Terminal Horizon Repeating Forever"
        val track = sampleTracks.first().copy(title = longTitle)
        val combined = "${track.artist} :: $longTitle · 0 PLAY(S)"
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.width(320.dp)) {
                    SearchScreen(listOf(track), emptyList(), {}, {}, {}, initialQuery = "Endless")
                }
            }
        }

        val resultNode = compose.onNodeWithText(combined)
        val layout = resultNode.textLayoutResult()
        assertEquals(1, layout.lineCount)
        assertTrue(layout.hasVisualOverflow)
    }

    private fun SemanticsNodeInteraction.textLayoutResult(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        return results.single()
    }
}
