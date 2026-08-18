package ca.stewark.nocturnel.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.FavoriteAlbumEntity
import ca.stewark.nocturnel.data.entity.FavoriteTrackEntity
import ca.stewark.nocturnel.data.entity.PlayHistoryEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.data.model.AlbumPlayCountRow
import ca.stewark.nocturnel.data.model.ListeningHistoryRow
import ca.stewark.nocturnel.data.model.TrackPlayCountRow
import kotlinx.coroutines.flow.Flow

@Dao
interface ListeningDao {
    @Query("SELECT relativePath FROM favorite_tracks ORDER BY favoritedAtEpochMillis DESC")
    fun favoriteTrackPaths(): Flow<List<String>>

    @Query("SELECT albumId FROM favorite_albums ORDER BY favoritedAtEpochMillis DESC")
    fun favoriteAlbumIds(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE relativePath = :path)")
    suspend fun isFavoriteTrack(path: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_albums WHERE albumId = :albumId)")
    suspend fun isFavoriteAlbum(albumId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteTrack(favorite: FavoriteTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteAlbum(favorite: FavoriteAlbumEntity)

    @Query("DELETE FROM favorite_tracks WHERE relativePath = :path")
    suspend fun deleteFavoriteTrack(path: String)

    @Query("DELETE FROM favorite_albums WHERE albumId = :albumId")
    suspend fun deleteFavoriteAlbum(albumId: String)

    @Transaction
    suspend fun toggleFavoriteTrack(path: String, now: Long) {
        if (isFavoriteTrack(path)) deleteFavoriteTrack(path)
        else insertFavoriteTrack(FavoriteTrackEntity(path, now))
    }

    @Transaction
    suspend fun toggleFavoriteAlbum(albumId: String, now: Long) {
        if (isFavoriteAlbum(albumId)) deleteFavoriteAlbum(albumId)
        else insertFavoriteAlbum(FavoriteAlbumEntity(albumId, now))
    }

    @Query("""
        SELECT track.* FROM tracks AS track
        INNER JOIN favorite_tracks AS favorite ON favorite.relativePath = track.relativePath
        WHERE track.status = 'PLAYABLE'
        ORDER BY favorite.favoritedAtEpochMillis DESC
    """)
    fun favoriteTracks(): Flow<List<TrackEntity>>

    @Query("""
        SELECT album.* FROM albums AS album
        INNER JOIN favorite_albums AS favorite ON favorite.albumId = album.id
        WHERE EXISTS (SELECT 1 FROM tracks WHERE tracks.albumId = album.id AND tracks.status = 'PLAYABLE')
        ORDER BY favorite.favoritedAtEpochMillis DESC
    """)
    fun favoriteAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT relativePath, playCount FROM track_play_stats")
    fun trackPlayCounts(): Flow<List<TrackPlayCountRow>>

    @Query("""
        SELECT track.albumId AS albumId, SUM(stats.playCount) AS playCount
        FROM track_play_stats AS stats
        INNER JOIN tracks AS track ON track.relativePath = stats.relativePath
        GROUP BY track.albumId
    """)
    fun albumPlayCounts(): Flow<List<AlbumPlayCountRow>>

    @Query("""
        SELECT history.id, history.qualificationId, history.relativePath, history.playedAtEpochMillis,
               track.title, track.artist, track.album, track.albumId, track.durationMs, track.status, track.documentUri
        FROM play_history AS history
        LEFT JOIN tracks AS track ON track.relativePath = history.relativePath
        ORDER BY history.playedAtEpochMillis DESC, history.id DESC
    """)
    fun history(): Flow<List<ListeningHistoryRow>>

    @Query("""
        SELECT history.id, history.qualificationId, history.relativePath, history.playedAtEpochMillis,
               track.title, track.artist, track.album, track.albumId, track.durationMs, track.status, track.documentUri
        FROM play_history AS history
        INNER JOIN tracks AS track ON track.relativePath = history.relativePath
        WHERE track.status = 'PLAYABLE'
          AND history.id = (
              SELECT newer.id FROM play_history AS newer
              WHERE newer.relativePath = history.relativePath
              ORDER BY newer.playedAtEpochMillis DESC, newer.id DESC LIMIT 1
          )
        ORDER BY history.playedAtEpochMillis DESC, history.id DESC
        LIMIT :limit
    """)
    fun recentDistinct(limit: Int): Flow<List<ListeningHistoryRow>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(event: PlayHistoryEntity): Long

    @Query("""
        INSERT INTO track_play_stats(relativePath, playCount, lastPlayedAtEpochMillis)
        VALUES(:path, 1, :playedAt)
        ON CONFLICT(relativePath) DO UPDATE SET
            playCount = playCount + 1,
            lastPlayedAtEpochMillis = :playedAt
    """)
    suspend fun incrementStats(path: String, playedAt: Long)

    @Query("""
        DELETE FROM play_history WHERE id NOT IN (
            SELECT id FROM play_history
            ORDER BY playedAtEpochMillis DESC, id DESC LIMIT :limit
        )
    """)
    suspend fun pruneHistory(limit: Int)

    @Transaction
    suspend fun recordQualifiedPlay(qualificationId: String, path: String, playedAt: Long, historyLimit: Int = 200): Boolean {
        val inserted = insertHistory(PlayHistoryEntity(qualificationId = qualificationId, relativePath = path, playedAtEpochMillis = playedAt))
        if (inserted == -1L) return false
        incrementStats(path, playedAt)
        pruneHistory(historyLimit)
        return true
    }

    @Query("DELETE FROM play_history")
    suspend fun clearHistory()

    @Query("DELETE FROM track_play_stats")
    suspend fun clearStats()

    @Query("DELETE FROM favorite_tracks")
    suspend fun clearFavoriteTracks()

    @Query("DELETE FROM favorite_albums")
    suspend fun clearFavoriteAlbums()

    @Transaction
    suspend fun clearHistoryAndCounts() {
        clearHistory()
        clearStats()
    }

    @Transaction
    suspend fun clearAllListeningData() {
        clearHistoryAndCounts()
        clearFavoriteTracks()
        clearFavoriteAlbums()
    }
}
