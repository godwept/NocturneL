package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.ui.sampleAlbum
import ca.stewark.nocturnel.ui.sampleTracks
import ca.stewark.nocturnel.ui.playlist.AlbumPlaylistUiState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class AlbumDetailScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun addControlRequiresAPlayableTrack() {
        compose.setContent {
            NocturneLTheme {
                AlbumDetailScreen(sampleAlbum, sampleTracks.map { it.copy(status = "MISSING") }, {}, {}, {}, {}, {})
            }
        }

        compose.onNodeWithText("[ ADD TO PLAYLIST ]").assertIsNotEnabled()
    }

    @Test fun expandedPickerAndSuccessFeedbackAreInline() {
        var toggled = false
        compose.setContent {
            NocturneLTheme {
                AlbumDetailScreen(
                    album = sampleAlbum,
                    tracks = sampleTracks,
                    onBack = {},
                    onPlay = {},
                    onPlayAlbum = {},
                    onChooseArtwork = {},
                    onClearArtwork = {},
                    playlists = listOf(PlaylistEntity(1, "Night Run", 1)),
                    playlistPickerExpanded = true,
                    albumPlaylistState = AlbumPlaylistUiState.Success("ADDED 2 TRACK(S) TO NIGHT RUN"),
                    onTogglePlaylistPicker = { toggled = true },
                )
            }
        }

        compose.onNodeWithText("[ ADD TO PLAYLIST ]").assertIsEnabled().performClick()
        compose.onNodeWithText("ADD ALBUM TO PLAYLIST").assertIsDisplayed()
        compose.onNodeWithText(":: ADDED 2 TRACK(S) TO NIGHT RUN").assertIsDisplayed()
        assertTrue(toggled)
    }

    @Test fun albumAndTrackQueueActionsUsePlayableTracks() {
        var albumQueued = 0
        var trackQueued = ""
        compose.setContent {
            NocturneLTheme {
                AlbumDetailScreen(
                    sampleAlbum, sampleTracks, {}, {}, {}, {}, {},
                    onAddAlbumToQueue = { albumQueued = it.size },
                    onAddTrackToQueue = { trackQueued = it.relativePath },
                )
            }
        }

        compose.onNodeWithText("[ PLAY NEXT ]").assertDoesNotExist()
        compose.onNodeWithContentDescription("Play Carrier next").assertDoesNotExist()
        compose.onNodeWithText("[ ADD QUEUE ]").performClick()
        compose.onNodeWithContentDescription("Add Carrier to queue").performClick()
        assertEquals(2, albumQueued)
        assertEquals(sampleTracks.first().relativePath, trackQueued)

        val actionTops = listOf("[ BACK ]", "[ PLAY ]", "[ SHUFFLE ]", "[ ADD QUEUE ]")
            .map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot.top }
        assertTrue(actionTops.max() - actionTops.min() <= 1f)
    }

    @Test fun longTrackTitleIsOneEllipsizedSemanticLine() {
        val longTitle = "Carrier Across The Endless Terminal Horizon Repeating Forever"
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.width(320.dp)) {
                    AlbumDetailScreen(sampleAlbum, listOf(sampleTracks.first().copy(title = longTitle)), {}, {}, {}, {}, {})
                }
            }
        }

        val titleNode = compose.onNodeWithText(longTitle)
        titleNode.assertIsDisplayed()
        val layout = titleNode.textLayoutResult()
        assertEquals(1, layout.lineCount)
        assertTrue(layout.hasVisualOverflow)
    }

    private fun SemanticsNodeInteraction.textLayoutResult(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        return results.single()
    }
}
