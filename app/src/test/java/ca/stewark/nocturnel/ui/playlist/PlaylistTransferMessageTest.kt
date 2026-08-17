package ca.stewark.nocturnel.ui.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistTransferMessageTest {
    @Test fun transferOutcomesRemainDistinctAndConcise() {
        assertEquals("Playlist import failed", PlaylistTransferMessages.IMPORT_FAILED)
        assertEquals("Playlist export failed", PlaylistTransferMessages.EXPORT_FAILED)
        assertEquals("Playlist import cancelled", PlaylistTransferMessages.IMPORT_CANCELLED)
        assertEquals("Playlist export cancelled", PlaylistTransferMessages.EXPORT_CANCELLED)
        assertEquals("Playlist exported", PlaylistTransferMessages.PLAYLIST_EXPORTED)
        assertEquals("Exported 5 playlist(s).", PlaylistExportSummary(5).message)
        assertEquals(
            "Imported 2 playlist(s), 4 track(s); skipped 1 playlist(s), 3 track(s).",
            PlaylistImportSummary(2, 4, 1, 3).message,
        )
    }
}
