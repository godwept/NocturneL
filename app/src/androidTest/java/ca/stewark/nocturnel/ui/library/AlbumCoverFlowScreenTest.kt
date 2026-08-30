package ca.stewark.nocturnel.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.ui.sampleAlbum
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        setFlow(
            initiallySelectedId = "beta",
            onOpened = { opened += it },
            containerSize = DpSize(412.dp, 700.dp),
        )

        compose.onNodeWithTag("cover-flow-cover-gamma")
            .performSemanticsAction(SemanticsActions.OnClick) { it.invoke() }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithContentDescription("Selected Gamma, 3 of 3")
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Selected Gamma, 3 of 3").assertIsDisplayed()
        assertEquals(emptyList<String>(), opened)

        compose.onNodeWithTag("cover-flow-cover-gamma")
            .performSemanticsAction(SemanticsActions.OnClick) { it.invoke() }
        assertEquals(listOf("gamma"), opened)
    }

    @Test fun activeCoverUses340DpMaximum() {
        setFlow(initiallySelectedId = "beta", containerSize = DpSize(412.dp, 700.dp))

        compose.onNodeWithTag("cover-flow-cover-beta").assertWidthIsEqualTo(340.dp)
        compose.onNodeWithText("BETA").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add Beta to favorites").assertIsDisplayed()
    }

    @Test fun activeCoverShrinksOnNarrowViewport() {
        setFlow(initiallySelectedId = "beta", containerSize = DpSize(320.dp, 700.dp))

        compose.onNodeWithTag("cover-flow-cover-beta").assertWidthIsEqualTo(268.8.dp)
    }

    @Test fun layersPreviousAndNextCoversBehindActiveCover() {
        setFlow(initiallySelectedId = "beta", containerSize = DpSize(412.dp, 700.dp))

        val previous = compose.onNodeWithTag("cover-flow-cover-alpha").fetchSemanticsNode().boundsInRoot
        val active = compose.onNodeWithTag("cover-flow-cover-beta").fetchSemanticsNode().boundsInRoot
        val next = compose.onNodeWithTag("cover-flow-cover-gamma").fetchSemanticsNode().boundsInRoot

        assertTrue(previous.center.x < active.center.x)
        assertTrue(active.center.x < next.center.x)
        assertTrue(previous.right > active.left)
        assertTrue(next.left < active.right)
        assertTrue(active.width > previous.width)
        assertTrue(active.width > next.width)
    }

    @Test fun scrollingTransfersEmphasisToCenteredAlbum() {
        setFlow(
            initiallySelectedId = "beta",
            effectsEnabled = true,
            containerSize = DpSize(412.dp, 700.dp),
        )

        compose.onNodeWithTag("cover-flow-reel").performTouchInput { swipeLeft() }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Selected Gamma, 3 of 3").assertIsDisplayed()
        compose.onNodeWithText("GAMMA").assertIsDisplayed()
        val former = compose.onNodeWithTag("cover-flow-cover-beta").fetchSemanticsNode().boundsInRoot
        val active = compose.onNodeWithTag("cover-flow-cover-gamma").fetchSemanticsNode().boundsInRoot
        assertTrue(active.width > former.width)
    }

    @Test fun touchingExposedNeighborCentersBeforeOpening() {
        val opened = mutableListOf<String>()
        setFlow(
            initiallySelectedId = "beta",
            onOpened = { opened += it },
            containerSize = DpSize(412.dp, 700.dp),
        )
        val beta = compose.onNodeWithTag("cover-flow-cover-beta").fetchSemanticsNode().boundsInRoot
        val gamma = compose.onNodeWithTag("cover-flow-cover-gamma").fetchSemanticsNode().boundsInRoot
        val exposedCenter = Offset((maxOf(beta.right, gamma.left) + gamma.right) / 2f, gamma.center.y)

        compose.onRoot().performTouchInput { click(exposedCenter) }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Selected Gamma, 3 of 3").assertIsDisplayed()
        assertEquals(emptyList<String>(), opened)
        val centeredGamma = compose.onNodeWithTag("cover-flow-cover-gamma").fetchSemanticsNode().boundsInRoot
        compose.onRoot().performTouchInput { click(centeredGamma.center) }
        assertEquals(listOf("gamma"), opened)
    }

    @Test fun singleAlbumHasNoSidePlaceholders() {
        setFlow(albums = albums().take(1))

        compose.onNodeWithContentDescription("Selected Alpha, 1 of 1").assertIsDisplayed()
        compose.onNodeWithTag("cover-flow-cover-beta").assertDoesNotExist()
        compose.onNodeWithTag("cover-flow-cover-gamma").assertDoesNotExist()
    }

    @Test fun twoAlbumsKeepHardStopsAndOnlyAvailableNeighbor() {
        setFlow(albums = albums().take(2))

        compose.onNodeWithTag("cover-flow-reel").performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Selected Alpha, 1 of 2").assertIsDisplayed()
        compose.onNodeWithTag("cover-flow-reel").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Selected Beta, 2 of 2").assertIsDisplayed()
        compose.onNodeWithTag("cover-flow-reel").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Selected Beta, 2 of 2").assertIsDisplayed()
    }

    @Test fun compactPortraitKeepsMetadataAndControlsVisible() {
        setFlow(
            initiallySelectedId = "beta",
            containerSize = DpSize(320.dp, 640.dp),
        )

        compose.onNodeWithTag("cover-flow-cover-beta").assertIsDisplayed()
        compose.onNodeWithText("BETA").assertIsDisplayed()
        compose.onNodeWithText("Artist B").assertIsDisplayed()
        compose.onNodeWithText("0×").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add Beta to favorites").assertIsDisplayed()
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

    private fun albums() = listOf(
        sampleAlbum.copy(id = "alpha", title = "Alpha", artist = "Artist A"),
        sampleAlbum.copy(id = "beta", title = "Beta", artist = "Artist B"),
        sampleAlbum.copy(id = "gamma", title = "Gamma", artist = "Artist C"),
    )

    private fun setFlow(
        initiallySelectedId: String = "alpha",
        onOpened: (String) -> Unit = {},
        albums: List<AlbumEntity> = albums(),
        effectsEnabled: Boolean = false,
        containerSize: DpSize? = null,
    ) {
        compose.setContent {
            NocturneLTheme {
                var selectedId by remember { mutableStateOf<String?>(initiallySelectedId) }
                val containerModifier = containerSize?.let { Modifier.requiredSize(it.width, it.height) }
                    ?: Modifier.fillMaxSize()
                Box(containerModifier) {
                    AlbumCoverFlowScreen(
                        albums = albums,
                        state = rememberLazyListState(initialFirstVisibleItemIndex = albums.indexOfFirst { it.id == initiallySelectedId }),
                        favoriteAlbumIds = setOf("alpha"),
                        albumPlayCounts = mapOf("alpha" to 12),
                        effectsEnabled = effectsEnabled,
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
