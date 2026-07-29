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
import ca.stewark.nocturnel.data.model.PlaylistEntryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library_source WHERE id = 0") suspend fun librarySource(): LibrarySourceEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveLibrarySource(source: LibrarySourceEntity)
    @Query("DELETE FROM tracks") suspend fun clearTracks()
    @Query("DELETE FROM albums") suspend fun clearAlbums()
    @Query("DELETE FROM scan_issues") suspend fun clearScanIssues()
    @Query("DELETE FROM scan_reports") suspend fun clearScanReports()
    @Query("SELECT * FROM albums WHERE EXISTS (SELECT 1 FROM tracks WHERE tracks.albumId = albums.id AND tracks.status = 'PLAYABLE') ORDER BY artist, title")
    fun albums(): Flow<List<AlbumEntity>>
    @Query("SELECT * FROM tracks WHERE albumId = :albumId ORDER BY discNumber, trackNumber, title") fun tracksForAlbum(albumId: String): Flow<List<TrackEntity>>
    @Query("SELECT * FROM tracks WHERE relativePath = :path") suspend fun track(path: String): TrackEntity?
    @Query("SELECT * FROM tracks") suspend fun allTracks(): List<TrackEntity>
    @Query("SELECT * FROM tracks WHERE status = 'PLAYABLE' ORDER BY artist, album, discNumber, trackNumber, title") fun playableTracks(): Flow<List<TrackEntity>>
    @Query("SELECT * FROM albums WHERE id = :id") suspend fun album(id: String): AlbumEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAlbum(album: AlbumEntity)
    @Query("UPDATE albums SET manualArtworkUri = :uri WHERE id = :albumId") suspend fun updateManualArtwork(albumId: String, uri: String?)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTracks(tracks: List<TrackEntity>)
    @Query("UPDATE tracks SET status = 'MISSING' WHERE lastSeenScanEpochMillis < :scanEpochMillis") suspend fun markUnseenMissing(scanEpochMillis: Long): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveReport(report: ScanReportEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveIssues(issues: List<ScanIssueEntity>)
    @Query("SELECT * FROM playlists ORDER BY name") fun playlists(): Flow<List<PlaylistEntity>>
    @Query("SELECT * FROM playlists WHERE id = :id") suspend fun playlist(id: Long): PlaylistEntity?
    @Insert suspend fun createPlaylist(playlist: PlaylistEntity): Long
    @Query("UPDATE playlists SET name = :name, updatedEpochMillis = :updated WHERE id = :id") suspend fun renamePlaylist(id: Long, name: String, updated: Long)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun deletePlaylist(id: Long)
    @Query("DELETE FROM playlist_entries WHERE playlistId = :playlistId") suspend fun clearPlaylistEntries(playlistId: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePlaylistEntries(entries: List<PlaylistEntryEntity>)
    @Query("SELECT * FROM playlist_entries WHERE playlistId = :playlistId ORDER BY position") suspend fun playlistEntries(playlistId: Long): List<PlaylistEntryEntity>
    @Query("""
        SELECT entry.position AS position,
               entry.relativePath AS relativePath,
               track.title AS title,
               track.artist AS artist,
               track.durationMs AS durationMs,
               track.status AS trackStatus
        FROM playlist_entries AS entry
        LEFT JOIN tracks AS track ON track.relativePath = entry.relativePath
        WHERE entry.playlistId = :playlistId
        ORDER BY entry.position
    """)
    suspend fun playlistEntryRows(playlistId: Long): List<PlaylistEntryRow>
    @Query("SELECT * FROM tracks WHERE relativePath IN (:paths) AND status = 'PLAYABLE'") suspend fun tracksByPaths(paths: List<String>): List<TrackEntity>

    @Transaction
    suspend fun selectLibrarySource(source: LibrarySourceEntity, sourceChanged: Boolean) {
        if (sourceChanged) {
            clearTracks()
            clearAlbums()
            clearScanIssues()
            clearScanReports()
        }
        saveLibrarySource(source)
    }

    @Transaction
    suspend fun saveAlbumsPreservingManualArtwork(albums: List<AlbumEntity>) {
        albums.forEach { scanned ->
            saveAlbum(scanned.copy(manualArtworkUri = album(scanned.id)?.manualArtworkUri))
        }
    }

    @Transaction
    suspend fun replacePlaylistEntries(playlistId: Long, entries: List<PlaylistEntryEntity>) {
        clearPlaylistEntries(playlistId)
        if (entries.isNotEmpty()) savePlaylistEntries(entries)
    }

    @Transaction
    suspend fun deletePlaylistAndEntries(playlistId: Long) {
        clearPlaylistEntries(playlistId)
        deletePlaylist(playlistId)
    }

    @Transaction
    suspend fun replaceCompletedScan(
        albums: List<AlbumEntity>,
        tracks: List<TrackEntity>,
        report: ScanReportEntity,
        issues: List<ScanIssueEntity>,
    ) {
        saveAlbumsPreservingManualArtwork(albums)
        saveTracks(tracks)
        markUnseenMissing(report.scannedAtEpochMillis)
        saveReport(report)
        saveIssues(issues)
    }
}
