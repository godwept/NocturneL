package ca.stewark.nocturnel.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.LibrarySourceEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntryEntity
import ca.stewark.nocturnel.data.entity.ScanIssueEntity
import ca.stewark.nocturnel.data.entity.ScanReportEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library_source WHERE id = 0") suspend fun librarySource(): LibrarySourceEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveLibrarySource(source: LibrarySourceEntity)
    @Query("SELECT * FROM albums ORDER BY artist, title") fun albums(): Flow<List<AlbumEntity>>
    @Query("SELECT * FROM tracks WHERE albumId = :albumId ORDER BY discNumber, trackNumber, title") fun tracksForAlbum(albumId: String): Flow<List<TrackEntity>>
    @Query("SELECT * FROM tracks WHERE relativePath = :path") suspend fun track(path: String): TrackEntity?
    @Query("SELECT * FROM tracks") suspend fun allTracks(): List<TrackEntity>
    @Query("SELECT * FROM tracks WHERE status = 'PLAYABLE' ORDER BY artist, album, discNumber, trackNumber, title") fun playableTracks(): Flow<List<TrackEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAlbums(albums: List<AlbumEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTracks(tracks: List<TrackEntity>)
    @Query("UPDATE tracks SET status = 'MISSING' WHERE lastSeenScanEpochMillis < :scanEpochMillis") suspend fun markUnseenMissing(scanEpochMillis: Long): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveReport(report: ScanReportEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveIssues(issues: List<ScanIssueEntity>)
    @Query("SELECT * FROM playlists ORDER BY name") fun playlists(): Flow<List<PlaylistEntity>>
    @Insert suspend fun createPlaylist(playlist: PlaylistEntity): Long
    @Query("UPDATE playlists SET name = :name, updatedEpochMillis = :updated WHERE id = :id") suspend fun renamePlaylist(id: Long, name: String, updated: Long)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: Long)
    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId") suspend fun clearPlaylistEntries(playlistId: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePlaylistEntries(entries: List<PlaylistEntryEntity>)
    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position") suspend fun playlistEntries(playlistId: Long): List<PlaylistEntryEntity>

    @Transaction
    suspend fun replaceCompletedScan(
        albums: List<AlbumEntity>,
        tracks: List<TrackEntity>,
        report: ScanReportEntity,
        issues: List<ScanIssueEntity>,
    ) {
        saveAlbums(albums)
        saveTracks(tracks)
        markUnseenMissing(report.scannedAtEpochMillis)
        saveReport(report)
        saveIssues(issues)
    }
}
