package ca.stewark.nocturnel.ui.playback

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.playback.QueueEntry
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QueueEditorScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun currentAndDuplicateUpcomingOccurrencesHaveEditorControls() {
        val moved = mutableListOf<String>()
        val state = queueEditorState(entry("current"), listOf(entry("first"), entry("second")), true, "REMOVED SECOND")
        compose.setContent {
            NocturneLTheme {
                QueueEditorScreen(state, {}, {}, { id, _, _ -> moved += id }, {}, {}, {}, {})
            }
        }

        compose.onNodeWithText("CURRENT").assertIsDisplayed()
        compose.onNodeWithText("UPCOMING").assertIsDisplayed()
        compose.onNodeWithContentDescription("Jump to first").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove second").assertIsDisplayed()
        compose.onNodeWithText("[ CLEAR UPCOMING ]").assertIsDisplayed()
        compose.onNodeWithText("[ UNDO ]").assertIsDisplayed()
    }

    @Test fun removeUndoAndClearRequireExplicitActions() {
        var removed = ""
        var undo = 0
        var clear = 0
        val state = queueEditorState(entry("current"), listOf(entry("next")), true, null)
        compose.setContent {
            NocturneLTheme {
                QueueEditorScreen(state, {}, {}, { _, _, _ -> }, { removed = it }, { undo++ }, { clear++ }, {})
            }
        }

        compose.onNodeWithContentDescription("Remove next").performClick()
        compose.onNodeWithText("[ UNDO ]").performClick()
        compose.onNodeWithText("[ CLEAR UPCOMING ]").performClick()
        compose.onNodeWithText("[ CONFIRM CLEAR ]").performClick()
        assertEquals("next", removed)
        assertEquals(1, undo)
        assertEquals(1, clear)
    }

    private fun entry(id: String) = QueueEntry(id, "same.flac", id, "Artist", "Album", 1_000)
}
