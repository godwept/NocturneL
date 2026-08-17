package ca.stewark.nocturnel.ui.playlist

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaylistIndexScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyIndexOffersImportAndExportAll() {
        var imported = 0
        var exported = 0
        compose.setContent {
            NocturneLTheme {
                PlaylistIndexScreen(
                    emptyList(), null, "", {}, {}, { imported++ }, { exported++ }, {}, {}, {}, {},
                )
            }
        }

        compose.onNodeWithText("[ IMPORT ]").assertIsDisplayed().assertHasClickAction().performClick()
        compose.onNodeWithText("[ EXPORT ALL ]").assertIsDisplayed().assertIsEnabled().performClick()
        assertEquals(1, imported)
        assertEquals(1, exported)
    }

    @Test fun playlistActionsAndNoticeRemainVisible() {
        val playlist = PlaylistEntity(1, "Night Run", 1)
        compose.setContent {
            NocturneLTheme {
                PlaylistIndexScreen(
                    listOf(playlist), "Imported 1 playlist(s)", "New", {}, {}, {}, {}, {}, {}, {}, {},
                )
            }
        }

        compose.onNodeWithText("[ CREATE ]").assertIsDisplayed()
        compose.onNodeWithText("[ OPEN ]").assertIsDisplayed()
        compose.onNodeWithText("[ PLAY ]").assertIsDisplayed()
        compose.onNodeWithText("[ EXPORT ]").assertIsDisplayed()
        compose.onNodeWithText("[ DELETE ]").assertIsDisplayed()
        compose.onNodeWithText(":: Imported 1 playlist(s)").assertIsDisplayed()
    }
}
