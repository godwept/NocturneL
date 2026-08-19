package ca.stewark.nocturnel.ui.listening

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import ca.stewark.nocturnel.data.model.ListeningHistoryRow
import ca.stewark.nocturnel.ui.sampleAlbum
import ca.stewark.nocturnel.ui.sampleTracks
import ca.stewark.nocturnel.ui.settings.SettingsScreen
import ca.stewark.nocturnel.ui.settings.TerminalSettingsState
import ca.stewark.nocturnel.ui.theme.NocturneLTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ListeningScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun landingOrdersResumeFavoritesRecentAndLibrary() {
        val track = sampleTracks.first()
        val resumeTitle = "Resume The Endless Terminal Carrier Across The Horizon Forever"
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
                    resume = ResumeUiState(resumeTitle, track.artist, 10_000, track.durationMs, true),
                    state = rememberLazyGridState(), {}, {}, {}, {}, {}, {}, {},
                )
            }
        }
        compose.onNodeWithText("RESUME").assertIsDisplayed()
        compose.onNodeWithText("+--[ FAVORITES ]").assertIsDisplayed()
        compose.onNodeWithText("+--[ RECENTLY PLAYED ]").assertIsDisplayed()
        compose.onNodeWithText("+--[ ALBUM LIBRARY ]").assertIsDisplayed()
        val resumeLayout = compose.onNodeWithText(resumeTitle).textLayoutResult()
        assertEquals(1, resumeLayout.lineCount)
        assertTrue(resumeLayout.hasVisualOverflow)
    }

    @Test fun listeningTrackTitleAndMetadataUseSingleEllipsizedLines() {
        val longTitle = "Carrier Across The Endless Terminal Horizon Repeating Forever"
        val longArtist = "The Extremely Long Terminal Ensemble Beyond The Horizon"
        val supporting = "A VERY LONG SUPPORTING STATUS THAT MUST NOT WRAP"
        compose.setContent {
            NocturneLTheme {
                Box(Modifier.width(240.dp)) {
                    ListeningTrackRow(longTitle, longArtist, 7, true, true, {}, {}, supportingText = supporting)
                }
            }
        }

        val titleLayout = compose.onNodeWithText(longTitle).textLayoutResult()
        val metadataLayout = compose.onNodeWithText("$longArtist · 7 PLAY(S) · $supporting").textLayoutResult()
        assertEquals(1, titleLayout.lineCount)
        assertEquals(1, metadataLayout.lineCount)
        assertTrue(titleLayout.hasVisualOverflow)
        assertTrue(metadataLayout.hasVisualOverflow)
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

    private fun SemanticsNodeInteraction.textLayoutResult(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(results) }
        return results.single()
    }
}
