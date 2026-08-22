package ca.stewark.nocturnel.ui.listening

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ca.stewark.nocturnel.ui.sampleAlbum
import ca.stewark.nocturnel.ui.settings.SettingsScreen
import ca.stewark.nocturnel.ui.settings.TerminalSettingsState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ListeningScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun landingShowsOnlyTheOrderedAlbumCollection() {
        val albums = listOf(
            sampleAlbum.copy(id = "alpha", title = "Alpha"),
            sampleAlbum.copy(id = "beta", title = "Beta"),
            sampleAlbum.copy(id = "gamma", title = "Gamma"),
        )
        compose.setContent {
            NocturneLTheme {
                LibraryLandingScreen(
                    albums = albums,
                    favoriteAlbumIds = setOf("gamma"),
                    albumPlayCounts = mapOf("gamma" to 3),
                    sortMode = LibrarySortMode.ARTIST,
                    viewMode = LibraryViewMode.GRID,
                    state = rememberLazyGridState(),
                    flowState = rememberLazyListState(),
                    effectsEnabled = false,
                    onAlbumSelected = {},
                    onFavoriteAlbum = {},
                    onCycleSort = {},
                    onToggleView = {},
                )
            }
        }

        albums.forEach { compose.onNodeWithText(it.title.uppercase()).assertIsDisplayed() }
        compose.onNodeWithContentDescription("Remove Gamma from favorites").assertIsDisplayed()
        compose.onNodeWithText("RESUME").assertDoesNotExist()
        compose.onNodeWithText("+--[ FAVORITES ]").assertDoesNotExist()
        compose.onNodeWithText("+--[ RECENTLY PLAYED ]").assertDoesNotExist()
        compose.onNodeWithText("VIEW ALL FAVORITES").assertDoesNotExist()
        compose.onNodeWithText("VIEW ALL HISTORY").assertDoesNotExist()
        compose.onNodeWithText("+--[ ALBUM LIBRARY ]").assertDoesNotExist()
    }

    @Test fun favoriteToggleImmediatelyMovesTheAlbum() {
        val albums = listOf(
            sampleAlbum.copy(id = "alpha", title = "Alpha"),
            sampleAlbum.copy(id = "beta", title = "Beta"),
            sampleAlbum.copy(id = "gamma", title = "Gamma"),
        )
        compose.setContent {
            NocturneLTheme {
                var favorites by remember { mutableStateOf(setOf("gamma")) }
                LibraryLandingScreen(
                    albums = albums,
                    favoriteAlbumIds = favorites,
                    albumPlayCounts = emptyMap(),
                    sortMode = LibrarySortMode.ARTIST,
                    viewMode = LibraryViewMode.GRID,
                    state = rememberLazyGridState(),
                    flowState = rememberLazyListState(),
                    effectsEnabled = false,
                    onAlbumSelected = {},
                    onFavoriteAlbum = { id ->
                        favorites = if (id in favorites) favorites - id else favorites + id
                    },
                    onCycleSort = {},
                    onToggleView = {},
                )
            }
        }

        assertEquals(listOf("GAMMA", "ALPHA", "BETA"), displayedAlbumOrder("ALPHA", "BETA", "GAMMA"))
        compose.onNodeWithContentDescription("Remove Gamma from favorites").performClick()
        assertEquals(listOf("ALPHA", "BETA", "GAMMA"), displayedAlbumOrder("ALPHA", "BETA", "GAMMA"))
    }

    @Test fun sortButtonCyclesModesAndImmediatelyReordersAlbums() {
        val albums = listOf(
            sampleAlbum.copy(id = "alpha", title = "Alpha", artist = "Zulu", year = "2020"),
            sampleAlbum.copy(id = "beta", title = "Beta", artist = "Alpha", year = "2022"),
            sampleAlbum.copy(id = "gamma", title = "Gamma", artist = "Middle", year = "2021"),
        )
        compose.setContent {
            NocturneLTheme {
                var mode by remember { mutableStateOf(LibrarySortMode.ARTIST) }
                LibraryLandingScreen(
                    albums = albums,
                    favoriteAlbumIds = emptySet(),
                    albumPlayCounts = mapOf("alpha" to 1, "beta" to 5, "gamma" to 10),
                    sortMode = mode,
                    viewMode = LibraryViewMode.GRID,
                    state = rememberLazyGridState(),
                    flowState = rememberLazyListState(),
                    effectsEnabled = false,
                    onAlbumSelected = {},
                    onFavoriteAlbum = {},
                    onCycleSort = { mode = mode.next() },
                    onToggleView = {},
                )
            }
        }

        compose.onNodeWithText("[ SORT: ARTIST ]").assertIsDisplayed()
        assertEquals(listOf("BETA", "GAMMA", "ALPHA"), displayedAlbumOrder("ALPHA", "BETA", "GAMMA"))
        compose.onNodeWithText("[ SORT: ARTIST ]").performClick()
        compose.onNodeWithText("[ SORT: TITLE ]").assertIsDisplayed()
        assertEquals(listOf("ALPHA", "BETA", "GAMMA"), displayedAlbumOrder("ALPHA", "BETA", "GAMMA"))
        compose.onNodeWithText("[ SORT: TITLE ]").performClick()
        compose.onNodeWithText("[ SORT: YEAR ]").assertIsDisplayed()
        assertEquals(listOf("BETA", "GAMMA", "ALPHA"), displayedAlbumOrder("ALPHA", "BETA", "GAMMA"))
        compose.onNodeWithText("[ SORT: YEAR ]").performClick()
        compose.onNodeWithText("[ SORT: MOST PLAYED ]").assertIsDisplayed()
        assertEquals(listOf("GAMMA", "BETA", "ALPHA"), displayedAlbumOrder("ALPHA", "BETA", "GAMMA"))
        compose.onNodeWithText("[ SORT: MOST PLAYED ]").performClick()
        compose.onNodeWithText("[ SORT: ARTIST ]").assertIsDisplayed()
    }

    @Test fun emptyLandingShowsTheExistingNotice() {
        compose.setContent {
            NocturneLTheme {
                LibraryLandingScreen(
                    albums = emptyList(),
                    favoriteAlbumIds = emptySet(),
                    albumPlayCounts = emptyMap(),
                    sortMode = LibrarySortMode.ARTIST,
                    viewMode = LibraryViewMode.GRID,
                    state = rememberLazyGridState(),
                    flowState = rememberLazyListState(),
                    effectsEnabled = false,
                    onAlbumSelected = {},
                    onFavoriteAlbum = {},
                    onCycleSort = {},
                    onToggleView = {},
                )
            }
        }

        compose.onNodeWithText("No playable albums yet. Rescan after adding music.").assertIsDisplayed()
        compose.onNodeWithText("[ SORT: ARTIST ]").assertDoesNotExist()
        compose.onNodeWithText("[ VIEW: GRID ]").assertDoesNotExist()
    }

    @Test fun viewButtonTogglesBetweenIndependentGridAndFlowViews() {
        val albums = listOf(
            sampleAlbum.copy(id = "alpha", title = "Alpha", artist = "Artist A"),
            sampleAlbum.copy(id = "beta", title = "Beta", artist = "Artist B"),
        )
        compose.setContent {
            NocturneLTheme {
                var mode by remember { mutableStateOf(LibraryViewMode.GRID) }
                LibraryLandingScreen(
                    albums = albums,
                    favoriteAlbumIds = emptySet(),
                    albumPlayCounts = emptyMap(),
                    sortMode = LibrarySortMode.ARTIST,
                    viewMode = mode,
                    state = rememberLazyGridState(),
                    flowState = rememberLazyListState(),
                    effectsEnabled = false,
                    onAlbumSelected = {},
                    onFavoriteAlbum = {},
                    onCycleSort = {},
                    onToggleView = { mode = mode.next() },
                )
            }
        }

        compose.onNodeWithText("[ VIEW: GRID ]").assertIsDisplayed().performClick()
        compose.onNodeWithText("[ VIEW: FLOW ]").assertIsDisplayed()
        compose.onNodeWithTag("cover-flow-reel").assertIsDisplayed()
        compose.onNodeWithText("[ VIEW: FLOW ]").performClick()
        compose.onNodeWithText("[ VIEW: GRID ]").assertIsDisplayed()
        compose.onNodeWithTag("cover-flow-reel").assertDoesNotExist()
    }

    @Test fun settingsConfirmsBothDestructiveActions() {
        var cleared = 0
        var changed = 0
        compose.setContent {
            NocturneLTheme {
                SettingsScreen({}, {}, TerminalSettingsState(), {}, onClearListeningData = { cleared++ }, pendingSourceName = "OTHER", onConfirmSourceChange = { changed++ })
            }
        }
        compose.onNodeWithText("[ CLEAR HISTORY + COUNTS ]").performClick()
        compose.onNodeWithText("[ CONFIRM CLEAR ]").performClick()
        compose.onNodeWithText("[ CONFIRM FOLDER CHANGE ]").performClick()
        assertEquals(1, cleared)
        assertEquals(1, changed)
    }

    private fun displayedAlbumOrder(vararg titles: String): List<String> = titles
        .map { title -> title to compose.onNodeWithText(title).fetchSemanticsNode().boundsInRoot }
        .sortedWith(compareBy({ it.second.top }, { it.second.left }))
        .map { it.first }
}
