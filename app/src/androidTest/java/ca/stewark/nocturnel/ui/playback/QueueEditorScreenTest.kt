package ca.stewark.nocturnel.ui.playback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.playback.QueueEntry
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test fun dragDownAcrossMultipleRowsCommitsOnceOnRelease() {
        val moves = mutableListOf<Move>()
        setQueue(listOf("first", "second", "third", "fourth")) { id, target, current ->
            moves += Move(id, target, current)
        }

        compose.onNodeWithTag("queue-drag-second").performTouchInput {
            down(center)
            moveBy(Offset(0f, center.y * 6f))
            up()
        }

        assertEquals(listOf(Move("second", 3, "current")), moves)
    }

    @Test fun dragUpAcrossMultipleRowsCommitsOnceOnRelease() {
        val moves = mutableListOf<Move>()
        setQueue(listOf("first", "second", "third", "fourth")) { id, target, current ->
            moves += Move(id, target, current)
        }

        compose.onNodeWithTag("queue-drag-fourth").performTouchInput {
            down(center)
            moveBy(Offset(0f, -center.y * 8f))
            up()
        }

        assertEquals(listOf(Move("fourth", 0, "current")), moves)
    }

    @Test fun noOpCancelledAndSingleItemDragsDoNotMove() {
        val moves = mutableListOf<Move>()
        setQueue(listOf("only")) { id, target, current -> moves += Move(id, target, current) }

        compose.onNodeWithTag("queue-drag-only").performTouchInput {
            down(center)
            moveBy(Offset(0f, 2f))
            up()
        }
        compose.onNodeWithTag("queue-drag-only").performTouchInput {
            down(center)
            moveBy(Offset(0f, center.y * 2f))
            cancel()
        }

        assertTrue(moves.isEmpty())
    }

    @Test fun staleCurrentTrackCancelsActiveDrag() {
        val moves = mutableListOf<Move>()
        var editorState by mutableStateOf(state("current", listOf("first", "second", "third")))
        compose.setContent {
            NocturneLTheme {
                QueueEditorScreen(editorState, {}, {}, { id, target, current -> moves += Move(id, target, current) }, {}, {}, {}, {})
            }
        }

        compose.onNodeWithTag("queue-drag-second").performTouchInput {
            down(center)
            moveBy(Offset(0f, center.y * 4f))
        }
        compose.runOnIdle { editorState = state("replacement", listOf("first", "second", "third")) }
        compose.onNodeWithTag("queue-drag-second").performTouchInput { up() }

        assertTrue(moves.isEmpty())
    }

    @Test fun accessibilityMovesAndRowActionsRemainIndependent() {
        val moves = mutableListOf<Move>()
        var jumped = ""
        var removed = ""
        val editorState = state("current", listOf("first", "second", "third"))
        compose.setContent {
            NocturneLTheme {
                QueueEditorScreen(
                    editorState,
                    {},
                    { jumped = it },
                    { id, target, current -> moves += Move(id, target, current) },
                    { removed = it },
                    {}, {}, {},
                )
            }
        }

        val actions = compose.onNodeWithTag("queue-drag-second")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle {
            actions.first { it.label == "Move second up" }.action()
            actions.first { it.label == "Move second down" }.action()
        }
        compose.onNodeWithContentDescription("Jump to second").performClick()
        compose.onNodeWithContentDescription("Remove second").performClick()

        assertEquals(
            listOf(Move("second", 0, "current"), Move("second", 2, "current")),
            moves,
        )
        assertEquals("second", jumped)
        assertEquals("second", removed)
    }

    @Test fun liftedRowExposesItsPreviewPosition() {
        compose.setContent {
            NocturneLTheme {
                UpcomingQueueRow(
                    row = queueEditorState(null, listOf(entry("dragged"), entry("next")), false, null).upcoming.first(),
                    currentOccurrenceId = "current",
                    isDragging = true,
                    dragTranslationY = 20f,
                    itemCount = 2,
                    onJump = {},
                    onMove = { _, _, _ -> },
                    onRemove = {},
                    onDragStart = {},
                    onDrag = {},
                    onDragEnd = {},
                    onDragCancel = {},
                )
            }
        }

        compose.onNodeWithTag("queue-row-dragged").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Dragging, position 1 of 2"),
        )
    }

    @Test fun currentAndUpcomingTrackTextUseSingleEllipsizedLines() {
        val currentTitle = "Current Carrier Across The Endless Terminal Horizon Forever"
        val upcomingTitle = "Upcoming Carrier Across The Endless Terminal Horizon Forever"
        val longArtist = "The Extremely Long Terminal Ensemble Beyond The Horizon"
        val longAlbum = "An Album Whose Name Continues Beyond The Visible Terminal"
        val editorState = queueEditorState(
            entry("current", currentTitle, longArtist, longAlbum),
            listOf(entry("upcoming", upcomingTitle, longArtist, longAlbum)),
            false,
            null,
        )
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.width(320.dp)) {
                    QueueEditorScreen(editorState, {}, {}, { _, _, _ -> }, {}, {}, {}, {})
                }
            }
        }

        val layouts = listOf(
            compose.onNodeWithText(currentTitle).textLayoutResult(),
            compose.onNodeWithText("$longArtist · $longAlbum").textLayoutResult(),
            compose.onNodeWithText(upcomingTitle).textLayoutResult(),
            compose.onNodeWithText("$longArtist · 0:01").textLayoutResult(),
        )
        assertTrue(layouts.all { it.lineCount == 1 })
        assertTrue(layouts.all { it.hasVisualOverflow })
        compose.onNodeWithContentDescription("Jump to $upcomingTitle").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove $upcomingTitle").assertIsDisplayed()
        compose.onNodeWithContentDescription("Reorder $upcomingTitle").assertIsDisplayed()
    }

    private fun setQueue(ids: List<String>, onMove: (String, Int, String?) -> Unit) {
        val editorState = state("current", ids)
        compose.setContent {
            NocturneLTheme { QueueEditorScreen(editorState, {}, {}, onMove, {}, {}, {}, {}) }
        }
    }

    private fun state(currentId: String, upcomingIds: List<String>) =
        queueEditorState(entry(currentId), upcomingIds.map(::entry), false, null)

    private data class Move(val occurrenceId: String, val targetIndex: Int, val currentOccurrenceId: String?)

    private fun entry(
        id: String,
        title: String = id,
        artist: String = "Artist",
        album: String = "Album",
    ) = QueueEntry(id, "same.flac", title, artist, album, 1_000)

    private fun SemanticsNodeInteraction.textLayoutResult(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        return results.single()
    }
}
