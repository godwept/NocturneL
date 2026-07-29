package ca.stewark.nocturnel.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.ui.settings.SettingsScreen
import ca.stewark.nocturnel.ui.settings.TerminalSettingsState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun libraryStartsWithAlbumsAndHasNoRescanFrame() {
        compose.setContent {
            NocturneLTheme {
                LibraryScreen(listOf(sampleAlbum)) {}
            }
        }

        compose.onNodeWithText(sampleAlbum.title).assertIsDisplayed()
        compose.onAllNodesWithText("[ RESCAN ]").assertCountEquals(0)
        compose.onAllNodesWithText("[ CANCEL ]").assertCountEquals(0)
    }

    @Test fun settingsRetainsRescanAction() {
        var rescanned = false
        compose.setContent {
            NocturneLTheme {
                SettingsScreen(
                    onChooseFolder = {},
                    onRescan = { rescanned = true },
                    state = TerminalSettingsState(),
                    onEffectsChanged = {},
                )
            }
        }

        compose.onNodeWithText("[ RESCAN LIBRARY ]").assertIsDisplayed().performClick()
        assertTrue(rescanned)
    }
}
