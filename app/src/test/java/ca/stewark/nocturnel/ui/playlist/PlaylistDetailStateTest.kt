package ca.stewark.nocturnel.ui.playlist

import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.data.model.PlaylistEntryRow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistDetailStateTest {
    @Test fun `mapper preserves order boundaries and available tracks`() {
        val playlist = PlaylistEntity(1, "Mix", 1)
        val rows = listOf(
            PlaylistEntryRow(0, "a", "A", "Artist", 1, "PLAYABLE"),
            PlaylistEntryRow(1, "b", "B", "Artist", 1, "PLAYABLE"),
        )
        val all = listOf(
            TrackEntity("a", "a", "x", "A", "Artist", "Album", 1, 1, 1, "PLAYABLE", 1),
            TrackEntity("c", "c", "x", "C", "Artist", "Album", 1, 1, 1, "PLAYABLE", 1),
        )
        val state = playlistDetailState(playlist, rows, all)
        assertFalse(state.entries.first().canMoveUp)
        assertFalse(state.entries.last().canMoveDown)
        assertTrue(state.availableTracks.single().relativePath == "c")
    }
}
