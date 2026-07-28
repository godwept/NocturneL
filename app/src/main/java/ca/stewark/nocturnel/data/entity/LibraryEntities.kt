package ca.stewark.nocturnel.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_source")
data class LibrarySourceEntity(
    @PrimaryKey val id: Int = 0,
    val treeUri: String,
    val displayName: String,
    val lastScanEpochMillis: Long?,
    val accessLost: Boolean,
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val relativeFolder: String,
    val title: String,
    val artist: String,
    val year: String?,
    val manualArtworkUri: String?,
    val folderArtworkUri: String?,
    val embeddedArtwork: ByteArray?,
)

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val relativePath: String,
    val documentUri: String,
    val albumId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val trackNumber: Int?,
    val discNumber: Int?,
    val status: String,
    val lastSeenScanEpochMillis: Long,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val updatedEpochMillis: Long)

@Entity(tableName = "playlist_entries", primaryKeys = ["playlistId", "position"])
data class PlaylistEntryEntity(val playlistId: Long, val position: Int, val relativePath: String)

@Entity(tableName = "scan_reports")
data class ScanReportEntity(
    @PrimaryKey val scannedAtEpochMillis: Long,
    val added: Int,
    val changed: Int,
    val missing: Int,
    val skipped: Int,
    val unsupported: Int,
)

@Entity(tableName = "scan_issues", primaryKeys = ["scannedAtEpochMillis", "relativePath"])
data class ScanIssueEntity(val scannedAtEpochMillis: Long, val relativePath: String, val message: String)

@Entity(tableName = "playback_preferences")
data class PlaybackPreferenceEntity(@PrimaryKey val key: String, val value: String)
