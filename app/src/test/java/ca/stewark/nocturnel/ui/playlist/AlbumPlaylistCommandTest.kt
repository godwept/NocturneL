package ca.stewark.nocturnel.ui.playlist

import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.playlist.AppendAlbumResult
import ca.stewark.nocturnel.playlist.PlaylistNotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumPlaylistCommandTest {
    @Test fun `add forwards only playable tracks in order`() = runTest {
        var captured = emptyList<String>()
        val command = AlbumPlaylistCommand(
            appendAlbum = { _, paths -> captured = paths; AppendAlbumResult(2, 0) },
            createPlaylist = { 1 },
        )

        val state = command.add(1, "Night Run", listOf(track("01", "PLAYABLE"), track("02", "MISSING"), track("03", "PLAYABLE")))

        assertEquals(listOf("01", "03"), captured)
        assertTrue(state is AlbumPlaylistUiState.Success)
    }

    @Test fun `create trims name and appends to returned playlist`() = runTest {
        var createdName = ""
        var appendedId = -1L
        val command = AlbumPlaylistCommand(
            appendAlbum = { id, _ -> appendedId = id; AppendAlbumResult(1, 0) },
            createPlaylist = { name -> createdName = name; 42L },
        )

        val state = command.createAndAdd("  Night Run  ", listOf(track("01", "PLAYABLE")))

        assertEquals("Night Run", createdName)
        assertEquals(42L, appendedId)
        assertTrue(state is AlbumPlaylistUiState.Success)
    }

    @Test fun `blank create name does not call dependencies`() = runTest {
        var called = false
        val command = AlbumPlaylistCommand(
            appendAlbum = { _, _ -> called = true; AppendAlbumResult(0, 0) },
            createPlaylist = { called = true; 1 },
        )

        val state = command.createAndAdd("  ", listOf(track("01", "PLAYABLE")))

        assertEquals(false, called)
        assertTrue(state is AlbumPlaylistUiState.Error)
    }

    @Test fun `repository failure maps to error state`() = runTest {
        val command = AlbumPlaylistCommand(
            appendAlbum = { _, _ -> error("write failed") },
            createPlaylist = { 1 },
        )

        assertTrue(command.add(1, "Night Run", listOf(track("01", "PLAYABLE"))) is AlbumPlaylistUiState.Error)
    }

    @Test fun `deleted target maps to warning state`() = runTest {
        val command = AlbumPlaylistCommand(
            appendAlbum = { id, _ -> throw PlaylistNotFoundException(id) },
            createPlaylist = { 1 },
        )

        assertTrue(command.add(1, "Night Run", listOf(track("01", "PLAYABLE"))) is AlbumPlaylistUiState.Warning)
    }

    @Test fun `append failure after creation is an error`() = runTest {
        val command = AlbumPlaylistCommand(
            appendAlbum = { id, _ -> throw PlaylistNotFoundException(id) },
            createPlaylist = { 42 },
        )

        assertTrue(
            command.createAndAdd("Night Run", listOf(track("01", "PLAYABLE"))) is
                AlbumPlaylistUiState.Error,
        )
    }

    private fun track(path: String, status: String) = TrackEntity(
        relativePath = path,
        documentUri = "content://$path",
        albumId = "album",
        title = path,
        artist = "Artist",
        album = "Album",
        durationMs = 1,
        trackNumber = 1,
        discNumber = 1,
        status = status,
        lastSeenScanEpochMillis = 1,
    )
}
