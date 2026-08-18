package ca.stewark.nocturnel.data

import ca.stewark.nocturnel.data.dao.ListeningDao
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.data.model.AlbumPlayCountRow
import ca.stewark.nocturnel.data.model.ListeningHistoryRow
import ca.stewark.nocturnel.data.model.TrackPlayCountRow
import kotlinx.coroutines.flow.Flow

interface ListeningStore {
    val favoriteTrackPaths: Flow<List<String>>
    val favoriteAlbumIds: Flow<List<String>>
    val favoriteTracks: Flow<List<TrackEntity>>
    val favoriteAlbums: Flow<List<AlbumEntity>>
    val trackPlayCounts: Flow<List<TrackPlayCountRow>>
    val albumPlayCounts: Flow<List<AlbumPlayCountRow>>
    val history: Flow<List<ListeningHistoryRow>>
    val recentDistinct: Flow<List<ListeningHistoryRow>>
    suspend fun toggleTrack(path: String)
    suspend fun toggleAlbum(albumId: String)
    suspend fun recordQualifiedPlay(qualificationId: String, path: String): Boolean
    suspend fun clearHistoryAndCounts()
}

class ListeningRepository(
    private val dao: ListeningDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ListeningStore {
    override val favoriteTrackPaths = dao.favoriteTrackPaths()
    override val favoriteAlbumIds = dao.favoriteAlbumIds()
    override val favoriteTracks = dao.favoriteTracks()
    override val favoriteAlbums = dao.favoriteAlbums()
    override val trackPlayCounts = dao.trackPlayCounts()
    override val albumPlayCounts = dao.albumPlayCounts()
    override val history = dao.history()
    override val recentDistinct = dao.recentDistinct(5)

    override suspend fun toggleTrack(path: String) = dao.toggleFavoriteTrack(path, nowMillis())
    override suspend fun toggleAlbum(albumId: String) = dao.toggleFavoriteAlbum(albumId, nowMillis())
    override suspend fun recordQualifiedPlay(qualificationId: String, path: String): Boolean =
        dao.recordQualifiedPlay(qualificationId, path, nowMillis())
    override suspend fun clearHistoryAndCounts() = dao.clearHistoryAndCounts()
}
