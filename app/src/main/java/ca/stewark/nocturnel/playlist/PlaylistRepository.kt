package ca.stewark.nocturnel.playlist

import ca.stewark.nocturnel.data.dao.LibraryDao
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntryEntity

class PlaylistRepository(private val dao: LibraryDao) {
    suspend fun create(name: String): Long = dao.createPlaylist(PlaylistEntity(name = name.trim().ifBlank { "Untitled playlist" }, updatedEpochMillis = System.currentTimeMillis()))

    suspend fun replaceEntries(playlistId: Long, paths: List<String>) {
        dao.clearPlaylistEntries(playlistId)
        dao.savePlaylistEntries(paths.mapIndexed { index, path -> PlaylistEntryEntity(playlistId, index, path) })
    }

    suspend fun rename(playlistId: Long, name: String) = dao.renamePlaylist(playlistId, name.trim().ifBlank { "Untitled playlist" }, System.currentTimeMillis())
    suspend fun delete(playlistId: Long) = dao.deletePlaylist(playlistId)

    suspend fun paths(playlistId: Long): List<String> = dao.playlistEntries(playlistId).map { it.relativePath }
}
