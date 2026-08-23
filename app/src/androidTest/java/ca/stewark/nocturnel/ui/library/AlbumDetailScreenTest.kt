package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.ui.sampleAlbum
import ca.stewark.nocturnel.ui.sampleTracks
import ca.stewark.nocturnel.ui.playlist.AlbumPlaylistUiState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import ca.stewark.nocturnel.ui.theme.FontPreset
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class AlbumDetailScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun addControlRequiresAPlayableTrack() {
        compose.setContent {
            NocturneLTheme {
                AlbumDetailScreen(sampleAlbum, sampleTracks.map { it.copy(status = "MISSING") }, {}, {}, {}, {})
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
                    playlists = listOf(PlaylistEntity(1, "Night Run", 1)),
                    playlistPickerExpanded = true,
                    albumPlaylistState = AlbumPlaylistUiState.Success("ADDED 2 TRACK(S) TO NIGHT RUN"),
                    onTogglePlaylistPicker = { toggled = true },
                )
            }
        }

        compose.onNodeWithText("[ ADD TO PLAYLIST ]").assertIsEnabled().performClick()
        compose.onNodeWithText("[ ADD ALBUM TO PLAYLIST ]").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(":: ADDED 2 TRACK(S) TO NIGHT RUN").performScrollTo().assertIsDisplayed()
        assertTrue(toggled)
    }

    @Test fun albumAndTrackQueueActionsUsePlayableTracks() {
        var albumQueued = 0
        var trackQueued = ""
        compose.setContent {
            NocturneLTheme {
                AlbumDetailScreen(
                    sampleAlbum, sampleTracks, {}, {}, {}, {},
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
                    AlbumDetailScreen(sampleAlbum, listOf(sampleTracks.first().copy(title = longTitle)), {}, {}, {}, {})
                }
            }
        }

        val titleNode = compose.onNodeWithText(longTitle)
        titleNode.assertIsDisplayed()
        val layout = titleNode.textLayoutResult()
        assertEquals(1, layout.lineCount)
        assertTrue(layout.hasVisualOverflow)
    }

    @Test fun trackActionsShareTheTitleLineWithoutExtraRowSpacing() {
        compose.setContent {
            NocturneLTheme {
                AlbumDetailScreen(sampleAlbum, sampleTracks, {}, {}, {}, {})
            }
        }

        val titleBounds = compose.onNodeWithText("Carrier").fetchSemanticsNode().boundsInRoot
        val favoriteBounds = compose.onNodeWithContentDescription("Add Carrier to favorites")
            .fetchSemanticsNode().boundsInRoot
        val queueBounds = compose.onNodeWithContentDescription("Add Carrier to queue")
            .fetchSemanticsNode().boundsInRoot
        val nextTitleBounds = compose.onNodeWithText("Afterimage").fetchSemanticsNode().boundsInRoot

        assertTrue(kotlin.math.abs(titleBounds.center.y - favoriteBounds.center.y) <= 1f)
        assertTrue(kotlin.math.abs(titleBounds.center.y - queueBounds.center.y) <= 1f)
        assertTrue(nextTitleBounds.top - titleBounds.top <= favoriteBounds.height + 1f)
    }

    @Test fun albumMetadataActionsWrapWithoutClippingOrClear() {
        var favoriteToggles = 0
        var coverSelections = 0
        var playlistToggles = 0
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.width(412.dp).testTag("album-detail-width")) {
                    AlbumDetailScreen(
                        album = sampleAlbum.copy(manualArtworkUri = "content://manual"),
                        tracks = sampleTracks,
                        onBack = {},
                        onPlay = {},
                        onPlayAlbum = {},
                        onChooseArtwork = { coverSelections++ },
                        albumPlayCount = 7,
                        onToggleAlbumFavorite = { favoriteToggles++ },
                        onTogglePlaylistPicker = { playlistToggles++ },
                    )
                }
            }
        }

        compose.onNodeWithText("[ CLEAR ]").assertDoesNotExist()
        val playCountBounds = compose.onNodeWithText("7 PLAY(S)").fetchSemanticsNode().boundsInRoot
        val labels = listOf(
            compose.onAllNodesWithText("[ FAV ]", useUnmergedTree = true).onFirst(),
            compose.onNodeWithText("[ SET COVER ]", useUnmergedTree = true),
            compose.onNodeWithText("[ ADD TO PLAYLIST ]", useUnmergedTree = true),
        )
        val labelBounds = labels.map { it.fetchSemanticsNode().boundsInRoot }
        assertTrue(playCountBounds.bottom <= labelBounds.minOf { it.top })
        val containerRight = compose.onNodeWithTag("album-detail-width").fetchSemanticsNode().boundsInRoot.right
        assertTrue(labelBounds.all { it.right <= containerRight + 1f })
        labels.forEach { node ->
            val layout = node.textLayoutResult()
            assertEquals(1, layout.lineCount)
        }

        compose.onNodeWithContentDescription("Add ${sampleAlbum.title} to favorites").performClick()
        compose.onNodeWithText("[ SET COVER ]").performClick()
        compose.onNodeWithText("[ ADD TO PLAYLIST ]").performClick()
        assertEquals(1, favoriteToggles)
        assertEquals(1, coverSelections)
        assertEquals(1, playlistToggles)
    }

    @Test fun albumActionRowsDoNotClipWithPixelFontAt320Dp() {
        compose.setContent {
            NocturneLTheme(fontPreset = FontPreset.PIXEL) {
                Box(Modifier.width(320.dp).testTag("album-detail-width")) {
                    AlbumDetailScreen(sampleAlbum, sampleTracks, {}, {}, {}, {})
                }
            }
        }

        val rootRight = compose.onNodeWithTag("album-detail-width").fetchSemanticsNode().boundsInRoot.right
        val actions = listOf(
            "BACK" to compose.onNodeWithText("[ BACK ]", useUnmergedTree = true),
            "PLAY" to compose.onNodeWithText("[ PLAY ]", useUnmergedTree = true),
            "SHUFFLE" to compose.onNodeWithText("[ SHUFFLE ]", useUnmergedTree = true),
            "ADD QUEUE" to compose.onNodeWithText("[ ADD QUEUE ]", useUnmergedTree = true),
            "FAV" to compose.onAllNodesWithText("[ FAV ]", useUnmergedTree = true).onFirst(),
            "SET COVER" to compose.onNodeWithText("[ SET COVER ]", useUnmergedTree = true),
            "ADD TO PLAYLIST" to compose.onNodeWithText("[ ADD TO PLAYLIST ]", useUnmergedTree = true),
        )
        actions.forEach { (label, action) ->
            action.assertIsDisplayed()
            assertEquals("$label must stay on one line", 1, action.textLayoutResult().lineCount)
            assertTrue(action.fetchSemanticsNode().boundsInRoot.right <= rootRight + 1f)
        }
    }

    private fun SemanticsNodeInteraction.textLayoutResult(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        return results.single()
    }
}
