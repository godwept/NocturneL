package ca.stewark.nocturnel.ui.playback

import ca.stewark.nocturnel.playback.QueueEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueEditorStateTest {
    @Test fun projectionKeepsDuplicateOccurrencesAndMoveAvailability() {
        val rows = listOf(entry("first"), entry("second"))
        val state = queueEditorState(
            current = entry("current"),
            upcoming = rows,
            canUndo = true,
            notice = "REMOVED",
        )

        assertEquals("current", state.current?.occurrenceId)
        assertEquals(listOf("first", "second"), state.upcoming.map { it.track.occurrenceId })
        assertFalse(state.upcoming.first().canMoveUp)
        assertTrue(state.upcoming.first().canMoveDown)
        assertTrue(state.upcoming.last().canMoveUp)
        assertFalse(state.upcoming.last().canMoveDown)
        assertTrue(state.canClear)
        assertTrue(state.canUndo)
    }

    @Test fun emptyProjectionCannotClear() {
        assertFalse(queueEditorState(null, emptyList(), false, null).canClear)
    }

    private fun entry(id: String) = QueueEntry(id, "same.flac", id, "Artist", "Album", 1_000)
}
