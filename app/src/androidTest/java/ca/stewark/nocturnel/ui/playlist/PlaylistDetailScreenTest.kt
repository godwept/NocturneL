package ca.stewark.nocturnel.ui.playlist

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
import ca.stewark.nocturnel.data.model.PlaylistEntryRow
import ca.stewark.nocturnel.ui.samplePlaylist
import ca.stewark.nocturnel.ui.sampleTracks
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import ca.stewark.nocturnel.ui.theme.FontPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaylistDetailScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun playlistRowsAreCompactAndAvailableRowsKeepAddActions() {
        val availableTrack = sampleTracks[1].copy(
            title = "Available Carrier",
            artist = "Available Artist",
        )
        val rows = listOf(
            PlaylistEntryRow(0, sampleTracks[0].relativePath, "Carrier", "Playlist Artist", 1_000, "PLAYABLE"),
            PlaylistEntryRow(1, "missing.flac", "Missing", "Artist", 1_000, "MISSING"),
        )
        val state = playlistDetailState(samplePlaylist, rows, listOf(sampleTracks[0], availableTrack))
        var queued = 0
        var skipped = 0
        var added = ""
        compose.setContent {
            NocturneLTheme {
                PlaylistDetailScreen(
                    state = state,
                    onBack = {},
                    onPlay = {},
                    onRename = {},
                    onAdd = { added = it },
                    onRemove = {},
                    onMove = { _, _ -> },
                    onAddToQueue = { tracks, skippedCount -> queued = tracks.size; skipped = skippedCount },
                )
            }
        }

        compose.onNodeWithText("[ PLAY NEXT ]").assertDoesNotExist()
        compose.onNodeWithContentDescription("Play Carrier next").assertDoesNotExist()
        compose.onNodeWithText("Playlist Artist").assertIsDisplayed()
        compose.onNodeWithText("Carrier").assertIsDisplayed()
        compose.onNodeWithText("Playlist Artist :: Carrier").assertDoesNotExist()
        compose.onNodeWithContentDescription("Reorder Carrier").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove Carrier").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add Carrier to queue").assertDoesNotExist()
        compose.onNodeWithText("Artist · 0:01").assertDoesNotExist()
        compose.onNodeWithText("[ ↑ ]").assertDoesNotExist()
        compose.onNodeWithText("[ ↓ ]").assertDoesNotExist()
        compose.onNodeWithText("[ ADD QUEUE ]").performClick()
        assertEquals(1, queued)
        assertEquals(1, skipped)

        compose.onNodeWithText("[ ADD TRACK ]").performClick()
        compose.onNodeWithText("Available Artist").assertIsDisplayed()
        compose.onNodeWithText("Available Carrier").assertIsDisplayed()
        compose.onNodeWithText("Available Artist :: Available Carrier").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add ${availableTrack.title}").performClick()
        assertEquals(availableTrack.relativePath, added)

        val backTop = compose.onNodeWithText("[ BACK ]").fetchSemanticsNode().boundsInRoot.top
        val actionTops = listOf("[ PLAY ]", "[ RENAME ]", "[ CLOSE ADD ]", "[ ADD QUEUE ]")
            .map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertTrue(backTop < actionTops.min())
        assertTrue(actionTops.max() - actionTops.min() <= 1f)
        assertEquals(1, compose.onNodeWithText("[ ADD QUEUE ]").textLayoutResult().lineCount)
    }

    @Test fun playlistTrackLabelEllipsizesArtistAndTitleIndependently() {
        val longTitle = "Carrier Across The Endless Terminal Horizon Repeating Forever"
        val longArtist = "The Extremely Long Terminal Ensemble Beyond The Horizon"
        compose.setContent {
            NocturneLTheme {
                PlaylistTrackLabel(
                    artist = longArtist,
                    title = longTitle,
                    modifier = Modifier.width(160.dp),
                )
            }
        }

        compose.onNodeWithText(longArtist).assertIsDisplayed()
        compose.onNodeWithText(longTitle).assertIsDisplayed()
        val artistLayout = compose.onNodeWithText(longArtist).textLayoutResult()
        val titleLayout = compose.onNodeWithText(longTitle).textLayoutResult()
        assertEquals(1, artistLayout.lineCount)
        assertEquals(1, titleLayout.lineCount)
        assertTrue(artistLayout.hasVisualOverflow)
        assertTrue(titleLayout.hasVisualOverflow)
    }

    @Test fun playlistActionsDoNotClipWithPixelFontAt320Dp() {
        compose.setContent {
            NocturneLTheme(fontPreset = FontPreset.PIXEL) {
                androidx.compose.foundation.layout.Box(Modifier.width(320.dp).testTag("playlist-detail-width")) {
                    PlaylistDetailScreen(state(listOf("first")), {}, {}, {}, {}, {}, { _, _ -> })
                }
            }
        }

        val rootRight = compose.onNodeWithTag("playlist-detail-width").fetchSemanticsNode().boundsInRoot.right
        listOf("[ PLAY ]", "[ RENAME ]", "[ ADD TRACK ]", "[ ADD QUEUE ]").forEach { label ->
            val action = compose.onNodeWithText(label, useUnmergedTree = true)
            action.assertIsDisplayed()
            assertEquals("$label must stay on one line", 1, action.textLayoutResult().lineCount)
            assertTrue(action.fetchSemanticsNode().boundsInRoot.right <= rootRight + 1f)
        }
    }

    @Test fun dragDownAcrossMultipleRowsCommitsOnce() {
        val downMoves = mutableListOf<Move>()
        setPlaylist(listOf("first", "second", "third", "fourth")) { from, to -> downMoves += Move(from, to) }
        compose.onNodeWithTag("playlist-drag-1").performTouchInput {
            down(center)
            moveBy(Offset(0f, center.y * 6f))
            up()
        }
        assertEquals(listOf(Move(1, 3)), downMoves)
    }

    @Test fun dragUpAcrossMultipleRowsCommitsOnce() {
        val upMoves = mutableListOf<Move>()
        setPlaylist(listOf("first", "second", "third", "fourth")) { from, to -> upMoves += Move(from, to) }
        val firstTop = compose.onNodeWithTag("playlist-row-0")
            .fetchSemanticsNode().boundsInRoot.top
        val fourthBounds = compose.onNodeWithTag("playlist-drag-3")
            .fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("playlist-drag-3").performTouchInput {
            down(center)
            moveTo(Offset(center.x, firstTop - fourthBounds.top - 1f))
            up()
        }
        assertEquals(listOf(Move(3, 0)), upMoves)
    }

    @Test fun cancelledAndNoOpSingleItemDragsDoNotMove() {
        val moves = mutableListOf<Move>()
        setPlaylist(listOf("only")) { from, to -> moves += Move(from, to) }
        compose.onNodeWithTag("playlist-drag-0").performTouchInput {
            down(center); moveBy(Offset(0f, 2f)); up()
        }
        compose.onNodeWithTag("playlist-drag-0").performTouchInput {
            down(center); moveBy(Offset(0f, center.y * 2f)); cancel()
        }
        assertTrue(moves.isEmpty())
    }

    @Test fun stalePlaylistOrderCancelsActiveDrag() {
        val moves = mutableListOf<Move>()
        val changingState = mutableStateOf(state(listOf("first", "second", "third")))
        compose.setContent {
            NocturneLTheme {
                PlaylistDetailScreen(
                    changingState.value, {}, {}, {}, {}, {},
                    { from, to -> moves += Move(from, to) },
                )
            }
        }
        compose.onNodeWithTag("playlist-drag-1").performTouchInput {
            down(center); moveBy(Offset(0f, center.y * 4f))
        }
        compose.runOnIdle {
            val entries = changingState.value.entries
            changingState.value = changingState.value.copy(
                entries = listOf(entries[1], entries[0], entries[2]),
            )
        }
        compose.onNodeWithTag("playlist-drag-1").performTouchInput { up() }
        assertTrue(moves.isEmpty())
    }

    @Test fun duplicateAndUnavailableRowsKeepIndependentActions() {
        val rows = listOf(
            PlaylistEntryRow(0, "same.flac", "First", "Artist", 1_000, "MISSING"),
            PlaylistEntryRow(1, "same.flac", "Second", "Artist", 1_000, "MISSING"),
        )
        val moves = mutableListOf<Move>()
        var removed = -1
        compose.setContent {
            NocturneLTheme {
                PlaylistDetailScreen(
                    playlistDetailState(samplePlaylist, rows, emptyList()),
                    {}, {}, {}, {}, { removed = it }, { from, to -> moves += Move(from, to) },
                )
            }
        }

        compose.onNodeWithText("Second").assertIsDisplayed()
        compose.onNodeWithTag("playlist-drag-1").performTouchInput {
            down(center); moveBy(Offset(0f, -center.y * 4f)); up()
        }
        compose.onNodeWithContentDescription("Remove Second").performClick()
        assertEquals(listOf(Move(1, 0)), moves)
        assertEquals(1, removed)
    }

    @Test fun accessibilityMovesRemainAvailable() {
        val moves = mutableListOf<Move>()
        setPlaylist(listOf("first", "second", "third")) { from, to -> moves += Move(from, to) }
        val actions = compose.onNodeWithTag("playlist-drag-1")
            .fetchSemanticsNode().config[SemanticsActions.CustomActions]
        compose.runOnIdle {
            actions.first { it.label == "Move second up" }.action()
            actions.first { it.label == "Move second down" }.action()
        }
        assertEquals(listOf(Move(1, 0), Move(1, 2)), moves)
    }

    @Test fun liftedStateRemainsAvailable() {
        compose.setContent {
            NocturneLTheme {
                PlaylistTrackEntryRow(
                    row = state(listOf("dragged", "next")).entries.first(),
                    previewIndex = 0,
                    itemCount = 2,
                    isDragging = true,
                    dragTranslationY = 20f,
                    onMove = { _, _ -> },
                    onRemove = {},
                    onDragStart = {}, onDrag = {}, onDragEnd = {}, onDragCancel = {},
                )
            }
        }
        compose.onNodeWithTag("playlist-row-0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Dragging, position 1 of 2"),
        )
    }

    private fun setPlaylist(titles: List<String>, onMove: (Int, Int) -> Unit) {
        compose.setContent {
            NocturneLTheme {
                PlaylistDetailScreen(state(titles), {}, {}, {}, {}, {}, onMove)
            }
        }
    }

    private fun state(titles: List<String>) = playlistDetailState(
        samplePlaylist,
        titles.mapIndexed { index, title ->
            PlaylistEntryRow(index, "same.flac", title, "Artist", 1_000, "MISSING")
        },
        emptyList(),
    )

    private data class Move(val from: Int, val to: Int)

    private fun SemanticsNodeInteraction.textLayoutResult(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        return results.single()
    }
}
