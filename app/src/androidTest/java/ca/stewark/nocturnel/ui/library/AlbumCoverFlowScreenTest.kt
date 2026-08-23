package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import ca.stewark.nocturnel.ui.sampleAlbum
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AlbumCoverFlowScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun showsSelectedAlbumMetadataAndFavoriteState() {
        setFlow()

        compose.onNodeWithTag("cover-flow-reel").assertIsDisplayed()
        compose.onNodeWithContentDescription("Selected Alpha, 1 of 3").assertIsDisplayed()
        compose.onNodeWithText("> CURRENT_").assertIsDisplayed()
        compose.onNodeWithText("01 / 03").assertIsDisplayed()
        compose.onNodeWithText("ALPHA").assertIsDisplayed()
        compose.onNodeWithText("Artist A").assertIsDisplayed()
        compose.onNodeWithText("12×").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove Alpha from favorites").assertIsDisplayed()
    }

    @Test fun sideTapCentersAndSecondTapOpens() {
        val opened = mutableListOf<String>()
        setFlow(initiallySelectedId = "beta", onOpened = { opened += it })

        compose.onNodeWithTag("cover-flow-cover-gamma").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Selected Gamma, 3 of 3").assertIsDisplayed()
        assertEquals(emptyList<String>(), opened)

        compose.onNodeWithTag("cover-flow-cover-gamma").performClick()
        assertEquals(listOf("gamma"), opened)
    }

    @Test fun swipingStopsAtTheCollectionEdges() {
        setFlow()

        repeat(4) {
            compose.onNodeWithTag("cover-flow-reel").performTouchInput { swipeLeft() }
            compose.waitForIdle()
        }
        compose.onNodeWithText("03 / 03").assertIsDisplayed()
        repeat(4) {
            compose.onNodeWithTag("cover-flow-reel").performTouchInput { swipeRight() }
            compose.waitForIdle()
        }
        compose.onNodeWithText("01 / 03").assertIsDisplayed()
    }

    private fun setFlow(initiallySelectedId: String = "alpha", onOpened: (String) -> Unit = {}) {
        val albums = listOf(
            sampleAlbum.copy(id = "alpha", title = "Alpha", artist = "Artist A"),
            sampleAlbum.copy(id = "beta", title = "Beta", artist = "Artist B"),
            sampleAlbum.copy(id = "gamma", title = "Gamma", artist = "Artist C"),
        )
        compose.setContent {
            NocturneLTheme {
                var selectedId by remember { mutableStateOf<String?>(initiallySelectedId) }
                Box(Modifier.fillMaxSize()) {
                    AlbumCoverFlowScreen(
                        albums = albums,
                        state = rememberLazyListState(initialFirstVisibleItemIndex = albums.indexOfFirst { it.id == initiallySelectedId }),
                        favoriteAlbumIds = setOf("alpha"),
                        albumPlayCounts = mapOf("alpha" to 12),
                        effectsEnabled = false,
                        selectedAlbumId = selectedId,
                        onSelectedAlbumChanged = { selectedId = it },
                        onAlbumSelected = { onOpened(it.id) },
                        onToggleFavorite = {},
                    )
                }
            }
        }
    }
}
