package ca.stewark.nocturnel.library.model

enum class TrackStatus { PLAYABLE, UNSUPPORTED, MISSING, METADATA_ISSUE }

enum class ArtworkKind { MANUAL, EMBEDDED, FOLDER, PLACEHOLDER }

data class ArtworkSource(val kind: ArtworkKind, val uri: String? = null)

data class LibrarySource(
    val treeUri: String,
    val displayName: String,
    val lastScanEpochMillis: Long? = null,
    val accessLost: Boolean = false,
)

data class Album(
    val id: String,
    val relativeFolder: String,
    val title: String,
    val artist: String,
    val year: String? = null,
    val artwork: ArtworkSource = ArtworkSource(ArtworkKind.PLACEHOLDER),
)

data class Track(
    val relativePath: String,
    val documentUri: String,
    val albumId: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val status: TrackStatus = TrackStatus.PLAYABLE,
)

data class Playlist(val id: Long, val name: String, val updatedEpochMillis: Long)

data class PlaylistEntry(val playlistId: Long, val position: Int, val relativePath: String)

data class ScanIssue(val relativePath: String, val message: String)

data class ScanReport(
    val scannedAtEpochMillis: Long,
    val added: Int,
    val changed: Int,
    val missing: Int,
    val skipped: Int,
    val unsupported: Int,
    val issues: List<ScanIssue> = emptyList(),
)
