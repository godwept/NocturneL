package ca.stewark.nocturnel.ui.library

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.ui.playlist.AlbumPlaylistUiState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AlbumPlaylistPickerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun populatedPickerSelectsOnePlaylist() {
        val playlists = listOf(
            PlaylistEntity(1, "Night Run", 1),
            PlaylistEntity(2, "Deep Focus", 2),
        )
        var selected: PlaylistEntity? = null

        compose.setContent {
            NocturneLTheme {
                AlbumPlaylistPicker(playlists, AlbumPlaylistUiState.Idle, { selected = it }, {})
            }
        }

        compose.onNodeWithText("[ ADD ALBUM TO PLAYLIST ]").assertIsDisplayed()
        compose.onNodeWithText("[ NIGHT RUN ]").assertHasClickAction().performClick()
        compose.onNodeWithText("[ DEEP FOCUS ]").assertHasClickAction()
        assertEquals(playlists.first(), selected)
    }

    @Test fun workingPickerDisablesSelection() {
        compose.setContent {
            NocturneLTheme {
                AlbumPlaylistPicker(
                    listOf(PlaylistEntity(1, "Night Run", 1)),
                    AlbumPlaylistUiState.Working,
                    {},
                    {},
                )
            }
        }

        compose.onNodeWithText("[ NIGHT RUN ]").assertIsNotEnabled()
    }

    @Test fun emptyPickerCreatesNamedPlaylist() {
        var createdName: String? = null
        compose.setContent {
            NocturneLTheme {
                AlbumPlaylistPicker(emptyList(), AlbumPlaylistUiState.Idle, {}, { createdName = it })
            }
        }

        compose.onNodeWithTag("create-and-add-playlist").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput(" Night Run ")
        compose.onNodeWithTag("create-and-add-playlist").assertIsEnabled().performClick()
        assertEquals("Night Run", createdName)
    }

    @Test fun errorKeepsEmptyFormVisible() {
        compose.setContent {
            NocturneLTheme {
                AlbumPlaylistPicker(
                    emptyList(),
                    AlbumPlaylistUiState.Error("COULD NOT ADD ALBUM TO PLAYLIST"),
                    {},
                    {},
                )
            }
        }

        compose.onNodeWithTag("new-playlist-name").assertIsDisplayed()
        compose.onNodeWithText(":: COULD NOT ADD ALBUM TO PLAYLIST").assertIsDisplayed()
    }
}
