package ca.stewark.nocturnel.ui.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.ui.sampleAlbum
import ca.stewark.nocturnel.ui.sampleTracks
import ca.stewark.nocturnel.ui.playlist.AlbumPlaylistUiState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
}
