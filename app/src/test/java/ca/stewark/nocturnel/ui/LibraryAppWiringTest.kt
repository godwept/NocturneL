package ca.stewark.nocturnel.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAppWiringTest {
    @Test fun `library wires one album collection without listening subviews`() {
        val app = File("src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt").readText()

        assertTrue("favoriteAlbumIds = listening.favoriteAlbumIds" in app)
        assertTrue("albumPlayCounts = listening.albumPlayCounts" in app)
        assertEquals(2, "sortMode = settings.librarySortMode".occurrencesIn(app))
        assertEquals(2, "onCycleSort = settingsViewModel::cycleLibrarySortMode".occurrencesIn(app))
        assertEquals(1, "val libraryFlowState = rememberLazyListState()".occurrencesIn(app))
        assertEquals(2, "viewMode = settings.libraryViewMode".occurrencesIn(app))
        assertEquals(2, "flowState = libraryFlowState".occurrencesIn(app))
        assertEquals(4, "effectsEnabled = settings.effectiveEffectsEnabled".occurrencesIn(app))
        assertEquals(2, "onToggleView = settingsViewModel::toggleLibraryViewMode".occurrencesIn(app))
        assertFalse("librarySubview" in app)
        assertFalse("FavoritesScreen" in app)
        assertFalse("ListeningHistoryScreen" in app)
        assertFalse("resumeState(" in app)
    }

    @Test fun `unreachable library listening UI is removed`() {
        val listeningRoot = File("src/main/java/ca/stewark/nocturnel/ui/listening")
        assertFalse(File(listeningRoot, "FavoritesScreen.kt").exists())
        assertFalse(File(listeningRoot, "ListeningHistoryScreen.kt").exists())
        assertFalse(File(listeningRoot, "ListeningRows.kt").exists())

        val models = File(listeningRoot, "ListeningUiModels.kt").readText()
        assertFalse("ResumeUiState" in models)
        assertFalse("resumeState" in models)
        assertFalse("previewFavoriteAlbums" in models)
        assertFalse("previewFavoriteTracks" in models)
        assertFalse("previewRecentTracks" in models)
    }

    private fun String.occurrencesIn(source: String): Int =
        source.windowed(length).count { it == this }
}
