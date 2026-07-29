package ca.stewark.nocturnel.ui.playlist

import ca.stewark.nocturnel.playlist.AppendAlbumResult
import ca.stewark.nocturnel.playlist.PlaylistNotFoundException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumPlaylistUiStateTest {
    @Test fun `success text reports added and skipped counts`() {
        assertEquals(
            AlbumPlaylistUiState.Success("ADDED 3 TRACK(S) TO NIGHT RUN"),
            albumAppendResultState("Night Run", AppendAlbumResult(3, 0)),
        )
        assertEquals(
            AlbumPlaylistUiState.Success("ADDED 2 TRACK(S) TO NIGHT RUN · SKIPPED 1"),
            albumAppendResultState("Night Run", AppendAlbumResult(2, 1)),
        )
    }

    @Test fun `complete overlap is a no-op state`() {
        assertEquals(
            AlbumPlaylistUiState.AlreadyPresent("ALBUM ALREADY IN PLAYLIST"),
            albumAppendResultState("Night Run", AppendAlbumResult(0, 4)),
        )
    }

    @Test fun `missing playlist is a warning and other failures are errors`() {
        assertTrue(albumAppendFailureState(PlaylistNotFoundException(1)) is AlbumPlaylistUiState.Warning)
        assertTrue(albumAppendFailureState(IllegalStateException("boom")) is AlbumPlaylistUiState.Error)
    }
}
