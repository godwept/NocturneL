package ca.stewark.nocturnel.library

import ca.stewark.nocturnel.artwork.ArtworkResolver
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.library.model.ScanIssue
import ca.stewark.nocturnel.library.model.TrackStatus
import java.security.MessageDigest

enum class ScanOutcome { COMPLETED, CANCELLED, ACCESS_LOST }

sealed interface ScanProgress {
    data object Discovering : ScanProgress
    data class Indexing(val completed: Int, val total: Int) : ScanProgress
}

data class ScanResult(
    val albums: List<AlbumEntity>,
    val tracks: List<TrackEntity>,
    val issues: List<ScanIssue>,
    val skipped: Int,
    val unsupported: Int,
    val outcome: ScanOutcome,
)

interface LibraryScanEngine {
    fun canAccess(treeUri: String): Boolean

    fun scan(
        treeUri: String,
        scanEpochMillis: Long,
        cancelled: () -> Boolean = { false },
        onProgress: (ScanProgress) -> Unit = {},
        existingCatalog: ExistingCatalogSnapshot = ExistingCatalogSnapshot.Empty,
    ): ScanResult
}

class LibraryScanner(
    private val documents: LibraryDocumentEnumerator,
    private val metadataReader: MediaMetadataReader,
) : LibraryScanEngine {
    override fun canAccess(treeUri: String): Boolean = documents.canAccess(treeUri)

    override fun scan(
        treeUri: String,
        scanEpochMillis: Long,
        cancelled: () -> Boolean,
        onProgress: (ScanProgress) -> Unit,
        existingCatalog: ExistingCatalogSnapshot,
    ): ScanResult {
        onProgress(ScanProgress.Discovering)
        val discoveredDocuments = try {
            documents.enumerate(treeUri, cancelled)
        } catch (_: LibraryEnumerationAccessException) {
            return accessLostResult()
        }
        if (cancelled()) return cancelledResult()

        val folderCovers = discoveredDocuments
            .filter { ArtworkResolver.isFolderCoverFile(it.displayName) }
            .associateBy { it.relativePath.substringBeforeLast('/', "") }
        val tracks = mutableListOf<TrackEntity>()
        val audioDocuments = mutableListOf<DiscoveredDocument>()
        val metadataByPath = mutableMapOf<String, ReadMetadata>()
        val issues = mutableListOf<ScanIssue>()
        val reusablePaths = mutableSetOf<String>()
        var skipped = 0

        for ((index, discovered) in discoveredDocuments.withIndex()) {
            if (cancelled()) return cancelledResult()
            onProgress(ScanProgress.Indexing(index + 1, discoveredDocuments.size))
            if (!SupportedAudioFormats.isCandidateAudioFile(discovered.relativePath)) {
                if (!ArtworkResolver.isFolderCoverFile(discovered.displayName)) skipped += 1
                continue
            }

            audioDocuments += discovered
            val existing = existingCatalog.tracksByPath[discovered.relativePath]
            val reusable = existing != null &&
                existing.documentUri == discovered.documentUri &&
                existing.status == TrackStatus.PLAYABLE.name &&
                FileFingerprint(existing.fileSizeBytes, existing.lastModifiedEpochMillis).matches(discovered.fingerprint)

            if (reusable) {
                reusablePaths += discovered.relativePath
                tracks += existing.copy(
                    lastSeenScanEpochMillis = scanEpochMillis,
                    fileSizeBytes = discovered.fileSizeBytes,
                    lastModifiedEpochMillis = discovered.lastModifiedEpochMillis,
                )
                continue
            }

            if (cancelled()) return cancelledResult()
            val fallback = MetadataFallbacks.fromPath(discovered.relativePath)
            val metadata = metadataReader.readTags(discovered.documentUri).getOrElse {
                issues += ScanIssue(discovered.relativePath, "Could not read media metadata")
                null
            }
            if (metadata != null) metadataByPath[discovered.relativePath] = metadata
            val folder = discovered.relativePath.substringBeforeLast('/', "")
            val albumId = albumId(folder)
            tracks += TrackEntity(
                relativePath = discovered.relativePath,
                documentUri = discovered.documentUri,
                albumId = albumId,
                title = MetadataFallbacks.preferred(metadata?.title, fallback.title),
                artist = MetadataFallbacks.preferred(metadata?.artist, fallback.artist),
                album = MetadataFallbacks.preferred(metadata?.album, fallback.album),
                durationMs = metadata?.durationMs ?: 0,
                trackNumber = metadata?.trackNumber ?: fallback.trackNumber,
                discNumber = metadata?.discNumber,
                status = if (metadata == null) TrackStatus.METADATA_ISSUE.name else TrackStatus.PLAYABLE.name,
                lastSeenScanEpochMillis = scanEpochMillis,
                fileSizeBytes = discovered.fileSizeBytes,
                lastModifiedEpochMillis = discovered.lastModifiedEpochMillis,
            )
        }

        val currentDocumentsByPath = audioDocuments.associateBy(DiscoveredDocument::relativePath)
        val currentTracksByAlbum = tracks.groupBy(TrackEntity::albumId)
        val previousTracksByAlbum = existingCatalog.tracksByPath.values.groupBy(TrackEntity::albumId)
        val albums = mutableListOf<AlbumEntity>()

        for ((id, albumTracks) in currentTracksByAlbum) {
            if (cancelled()) return cancelledResult()
            val firstTrack = albumTracks.first()
            val folder = firstTrack.relativePath.substringBeforeLast('/', "")
            val existingAlbum = existingCatalog.albumsById[id]
            val currentPaths = albumTracks.mapTo(linkedSetOf(), TrackEntity::relativePath)
            val previousPaths = previousTracksByAlbum[id].orEmpty().mapTo(linkedSetOf(), TrackEntity::relativePath)
            val currentFolderArtwork = folderCovers[folder]?.documentUri
            val clean = existingAlbum != null &&
                existingAlbum.folderArtworkUri == currentFolderArtwork &&
                AlbumScanPolicy.isClean(currentPaths, previousPaths, reusablePaths)

            if (clean) {
                albums += existingAlbum.copy(manualArtworkUri = null)
                continue
            }

            var embeddedArtwork: ByteArray? = null
            for (track in albumTracks) {
                if (cancelled()) return cancelledResult()
                val uri = currentDocumentsByPath.getValue(track.relativePath).documentUri
                val candidate = metadataReader.readArtwork(uri).getOrNull()
                if (candidate != null && candidate.isNotEmpty()) {
                    embeddedArtwork = candidate
                    break
                }
            }
            val firstMetadata = metadataByPath[firstTrack.relativePath]
            albums += AlbumEntity(
                id = id,
                relativeFolder = folder,
                title = firstTrack.album,
                artist = firstTrack.artist,
                year = firstMetadata?.year ?: existingAlbum?.year,
                manualArtworkUri = null,
                folderArtworkUri = currentFolderArtwork,
                embeddedArtwork = embeddedArtwork,
            )
        }

        if (cancelled()) return cancelledResult()
        return ScanResult(albums, tracks, issues, skipped, 0, ScanOutcome.COMPLETED)
    }

    private fun albumId(folder: String): String = MessageDigest.getInstance("SHA-256")
        .digest(folder.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun cancelledResult() = ScanResult(emptyList(), emptyList(), emptyList(), 0, 0, ScanOutcome.CANCELLED)
    private fun accessLostResult() = ScanResult(
        emptyList(),
        emptyList(),
        listOf(ScanIssue("", "Music folder is unavailable")),
        0,
        0,
        ScanOutcome.ACCESS_LOST,
    )
}
