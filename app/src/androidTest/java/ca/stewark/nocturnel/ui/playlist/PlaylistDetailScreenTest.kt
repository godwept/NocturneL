package ca.stewark.nocturnel.ui.playlist

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.data.model.PlaylistEntryRow
import ca.stewark.nocturnel.ui.samplePlaylist
import ca.stewark.nocturnel.ui.sampleTracks
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaylistDetailScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun playlistAndAvailableRowsExposeQueueActions() {
        val rows = listOf(
            PlaylistEntryRow(0, sampleTracks[0].relativePath, "Carrier", "Artist", 1_000, "PLAYABLE"),
            PlaylistEntryRow(1, "missing.flac", "Missing", "Artist", 1_000, "MISSING"),
        )
        val state = playlistDetailState(samplePlaylist, rows, sampleTracks)
        var queued = 0
        var skipped = 0
        compose.setContent {
            NocturneLTheme {
                PlaylistDetailScreen(
                    state, {}, {}, {}, {}, {}, { _, _ -> },
                    onAddToQueue = { tracks, skippedCount -> queued = tracks.size; skipped = skippedCount },
                )
            }
        }

        compose.onNodeWithText("[ ADD QUEUE ]").performClick()
        compose.onNodeWithContentDescription("Add Carrier to queue").performClick()
        compose.onNodeWithContentDescription("Add Missing to queue").assertDoesNotExist()
        assertEquals(1, queued)
        assertEquals(1, skipped)
    }
}
