package ca.stewark.nocturnel.ui.playlist

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
import ca.stewark.nocturnel.data.model.PlaylistEntryRow
import ca.stewark.nocturnel.ui.samplePlaylist
import ca.stewark.nocturnel.ui.sampleTracks
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
                    state = state,
                    onBack = {},
                    onPlay = {},
                    onRename = {},
                    onAdd = {},
                    onRemove = {},
                    onMove = { _, _ -> },
                    onAddToQueue = { tracks, skippedCount -> queued = tracks.size; skipped = skippedCount },
                )
            }
        }

        compose.onNodeWithText("[ PLAY NEXT ]").assertDoesNotExist()
        compose.onNodeWithContentDescription("Play Carrier next").assertDoesNotExist()
        compose.onNodeWithText("[ ADD QUEUE ]").performClick()
        compose.onNodeWithContentDescription("Add Carrier to queue").performClick()
        compose.onNodeWithContentDescription("Add Missing to queue").assertDoesNotExist()
        assertEquals(1, queued)
        assertEquals(1, skipped)

        val backTop = compose.onNodeWithText("[ BACK ]").fetchSemanticsNode().boundsInRoot.top
        val actionTops = listOf("[ PLAY ]", "[ RENAME ]", "[ ADD TRACK ]", "[ ADD QUEUE ]")
            .map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertTrue(backTop < actionTops.min())
        assertTrue(actionTops.max() - actionTops.min() <= 1f)
        assertEquals(1, compose.onNodeWithText("[ ADD QUEUE ]").textLayoutResult().lineCount)
    }

    @Test fun playlistTrackTextUsesSingleEllipsizedLines() {
        val longTitle = "Carrier Across The Endless Terminal Horizon Repeating Forever"
        val longArtist = "The Extremely Long Terminal Ensemble Beyond The Horizon"
        val track = sampleTracks.first().copy(title = longTitle, artist = longArtist)
        val rows = listOf(PlaylistEntryRow(0, track.relativePath, longTitle, longArtist, 1_000, "PLAYABLE"))
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.width(320.dp)) {
                    PlaylistDetailScreen(
                        state = playlistDetailState(samplePlaylist, rows, listOf(track)),
                        onBack = {}, onPlay = {}, onRename = {}, onAdd = {}, onRemove = {}, onMove = { _, _ -> },
                    )
                }
            }
        }

        compose.onNodeWithText("[ ADD TRACK ]").performClick()
        val availableLayout = compose.onNodeWithText("$longArtist :: $longTitle").textLayoutResult()
        val titleLayout = compose.onNodeWithText(longTitle).textLayoutResult()
        val metadataLayout = compose.onNodeWithText("$longArtist · 0:01").textLayoutResult()
        assertEquals(1, availableLayout.lineCount)
        assertEquals(1, titleLayout.lineCount)
        assertEquals(1, metadataLayout.lineCount)
        assertTrue(availableLayout.hasVisualOverflow)
        assertTrue(titleLayout.hasVisualOverflow)
        assertTrue(metadataLayout.hasVisualOverflow)
    }

    private fun SemanticsNodeInteraction.textLayoutResult(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        return results.single()
    }
}
