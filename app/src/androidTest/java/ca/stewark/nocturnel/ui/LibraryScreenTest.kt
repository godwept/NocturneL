package ca.stewark.nocturnel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import ca.stewark.nocturnel.ui.settings.SettingsScreen
import ca.stewark.nocturnel.ui.settings.TerminalSettingsState
import ca.stewark.nocturnel.ui.library.LibraryScanStatus
import ca.stewark.nocturnel.library.ScanProgress
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

        compose.onNodeWithText(sampleAlbum.title.uppercase()).assertIsDisplayed()
        compose.onAllNodesWithText("[ RESCAN ]").assertCountEquals(0)
        compose.onAllNodesWithText("[ CANCEL ]").assertCountEquals(0)
    }

    @Test fun libraryRestoresScrollPositionAfterReturningFromAnAlbum() {
        val albums = (0 until 20).map { index ->
            sampleAlbum.copy(id = "album-$index", title = "Album $index")
        }
        var gridState: LazyGridState? = null

        compose.setContent {
            NocturneLTheme {
                var selectedAlbum by remember { mutableStateOf(false) }
                val rememberedGridState = rememberLazyGridState()
                gridState = rememberedGridState
                if (selectedAlbum) {
                    Text("BACK", modifier = Modifier.clickable { selectedAlbum = false })
                } else {
                    LibraryScreen(albums, rememberedGridState) { selectedAlbum = true }
                }
            }
        }

        compose.onNode(hasScrollAction()).performScrollToIndex(12)
        compose.onNodeWithText("ALBUM 12").performClick()
        compose.onNodeWithText("BACK").performClick()

        compose.runOnIdle {
            assertTrue(gridState!!.firstVisibleItemIndex >= 12)
        }
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
                    onCycleFontPreset = {},
                )
            }
        }

        compose.onNodeWithText("[ RESCAN LIBRARY ]").assertIsDisplayed().performClick()
        assertTrue(rescanned)
    }

    @Test fun settingsShowsThatARescanTapWasAccepted() {
        compose.setContent {
            NocturneLTheme {
                SettingsScreen(
                    onChooseFolder = {},
                    onRescan = {},
                    scanRunning = true,
                    state = TerminalSettingsState(),
                    onEffectsChanged = {},
                    onCycleFontPreset = {},
                )
            }
        }

        compose.onNodeWithText("[ SCANNING... ]")
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test fun scanStatusShowsRealIndexingProgressAndCancels() {
        var cancelled = false
        compose.setContent {
            NocturneLTheme {
                LibraryScanStatus(ScanProgress.Indexing(completed = 4, total = 10), { cancelled = true })
            }
        }

        compose.onNodeWithText("INDEXING 4 OF 10 FILES").assertIsDisplayed()
        compose.onNodeWithText("[ CANCEL ]").assertIsDisplayed().performClick()
        assertTrue(cancelled)
    }

    @Test fun settingsShowsStructuredScanProgressAndCancels() {
        var cancelled = false
        compose.setContent {
            NocturneLTheme {
                SettingsScreen(
                    onChooseFolder = {},
                    onRescan = {},
                    state = TerminalSettingsState(),
                    onEffectsChanged = {},
                    onCycleFontPreset = {},
                    onCancelRescan = { cancelled = true },
                    scanProgress = ScanProgress.Indexing(completed = 8, total = 20),
                )
            }
        }

        compose.onNodeWithText("INDEXING 8 OF 20 FILES").assertIsDisplayed()
        compose.onNodeWithText("[ CANCEL ]").assertIsDisplayed().performClick()
        assertTrue(cancelled)
    }
}
