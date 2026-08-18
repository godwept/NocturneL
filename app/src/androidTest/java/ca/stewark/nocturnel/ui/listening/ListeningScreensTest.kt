package ca.stewark.nocturnel.ui.listening

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.stewark.nocturnel.data.model.ListeningHistoryRow
import ca.stewark.nocturnel.ui.sampleAlbum
import ca.stewark.nocturnel.ui.sampleTracks
import ca.stewark.nocturnel.ui.settings.SettingsScreen
import ca.stewark.nocturnel.ui.settings.TerminalSettingsState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ListeningScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun landingOrdersResumeFavoritesRecentAndLibrary() {
        val track = sampleTracks.first()
        val history = ListeningHistoryRow(1, "one", track.relativePath, 1, track.title, track.artist, track.album, track.albumId, track.durationMs, track.status, track.documentUri)
        compose.setContent {
            NocturneLTheme {
                LibraryLandingScreen(
                    albums = listOf(sampleAlbum),
                    listening = ListeningUiState(
                        favoriteAlbumIds = setOf(sampleAlbum.id), favoriteTrackPaths = setOf(track.relativePath),
                        favoriteAlbums = listOf(sampleAlbum), favoriteTracks = listOf(track), recentTracks = listOf(history),
                        trackPlayCounts = mapOf(track.relativePath to 3), albumPlayCounts = mapOf(sampleAlbum.id to 3),
                    ),
                    resume = ResumeUiState(track.title, track.artist, 10_000, track.durationMs, true),
                    state = rememberLazyGridState(), {}, {}, {}, {}, {}, {}, {},
                )
            }
        }
        compose.onNodeWithText("RESUME").assertIsDisplayed()
        compose.onNodeWithText("+--[ FAVORITES ]").assertIsDisplayed()
        compose.onNodeWithText("+--[ RECENTLY PLAYED ]").assertIsDisplayed()
        compose.onNodeWithText("+--[ ALBUM LIBRARY ]").assertIsDisplayed()
    }

    @Test fun emptyLandingOmitsDailySections() {
        compose.setContent {
            NocturneLTheme {
                LibraryLandingScreen(listOf(sampleAlbum), ListeningUiState(), null, rememberLazyGridState(), {}, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("RESUME").assertDoesNotExist()
        compose.onNodeWithText("+--[ FAVORITES ]").assertDoesNotExist()
        compose.onNodeWithText("+--[ RECENTLY PLAYED ]").assertDoesNotExist()
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
}
