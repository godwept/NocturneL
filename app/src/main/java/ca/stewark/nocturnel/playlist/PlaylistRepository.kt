package ca.stewark.nocturnel.playlist

import ca.stewark.nocturnel.data.dao.LibraryDao
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntryEntity

class PlaylistRepository(private val dao: LibraryDao) {
    suspend fun create(name: String): Long = dao.createPlaylist(PlaylistEntity(name = name.trim().ifBlank { "Untitled playlist" }, updatedEpochMillis = System.currentTimeMillis()))

    suspend fun replaceEntries(playlistId: Long, paths: List<String>) {
        dao.replacePlaylistEntries(playlistId, paths.mapIndexed { index, path -> PlaylistEntryEntity(playlistId, index, path) })
    }

    suspend fun add(playlistId: Long, relativePath: String, index: Int? = null) {
        val paths = paths(playlistId)
        replaceEntries(playlistId, PlaylistEditor.add(paths, relativePath, index ?: paths.size))
    }

    suspend fun removeAt(playlistId: Long, index: Int) =
        replaceEntries(playlistId, PlaylistEditor.removeAt(paths(playlistId), index))

    suspend fun move(playlistId: Long, fromIndex: Int, toIndex: Int) =
        replaceEntries(playlistId, PlaylistEditor.move(paths(playlistId), fromIndex, toIndex))

    suspend fun rename(playlistId: Long, name: String) = dao.renamePlaylist(playlistId, name.trim().ifBlank { "Untitled playlist" }, System.currentTimeMillis())
    suspend fun delete(playlistId: Long) = dao.deletePlaylistAndEntries(playlistId)

    suspend fun paths(playlistId: Long): List<String> = dao.playlistEntries(playlistId).map { it.relativePath }
    suspend fun playableTracks(playlistId: Long): List<ca.stewark.nocturnel.data.entity.TrackEntity> {
        val paths = paths(playlistId)
        val found = dao.tracksByPaths(paths).associateBy { it.relativePath }
        return paths.mapNotNull(found::get)
    }
}
