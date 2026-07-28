package ca.stewark.nocturnel.library

import android.net.Uri
import ca.stewark.nocturnel.artwork.ArtworkResolver
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.library.model.ScanIssue
import ca.stewark.nocturnel.library.model.TrackStatus
import java.security.MessageDigest

data class ScanResult(val albums: List<AlbumEntity>, val tracks: List<TrackEntity>, val issues: List<ScanIssue>, val skipped: Int, val unsupported: Int)

class LibraryScanner(
    private val treeAccess: LibraryTreeAccess,
    private val metadataReader: AndroidMediaMetadataReader,
) {
    fun scan(treeUri: String, scanEpochMillis: Long, cancelled: () -> Boolean = { false }, onProgress: (Int) -> Unit = {}): ScanResult {
        val root = treeAccess.openTree(treeUri) ?: return ScanResult(emptyList(), emptyList(), listOf(ScanIssue("", "Music folder is unavailable")), 0, 0)
        val albums = linkedMapOf<String, AlbumEntity>()
        val tracks = mutableListOf<TrackEntity>()
        val issues = mutableListOf<ScanIssue>()
        var skipped = 0
        var unsupported = 0
        var count = 0
        val documents = DocumentTreeWalker.walk(root, cancelled).toList()
        val folderCovers = documents.filter { ArtworkResolver.isFolderCoverFile(it.document.name.orEmpty()) }
            .associateBy { it.relativePath.substringBeforeLast('/', "") }
        documents.forEach { discovered ->
            if (cancelled()) return@forEach
            count += 1
            onProgress(count)
            if (!SupportedAudioFormats.isCandidateAudioFile(discovered.relativePath)) {
                if (!ArtworkResolver.isFolderCoverFile(discovered.document.name.orEmpty())) skipped += 1
                return@forEach
            }
            val fallback = MetadataFallbacks.fromPath(discovered.relativePath)
            val read = metadataReader.read(discovered.document.uri)
            val metadata = read.getOrElse {
                issues += ScanIssue(discovered.relativePath, "Could not read media metadata")
                null
            }
            val folder = discovered.relativePath.substringBeforeLast('/', "")
            val albumId = sha256(folder)
            val status = if (metadata == null) TrackStatus.METADATA_ISSUE else TrackStatus.PLAYABLE
            albums.putIfAbsent(albumId, AlbumEntity(
                albumId,
                folder,
                MetadataFallbacks.preferred(metadata?.album, fallback.album),
                MetadataFallbacks.preferred(metadata?.artist, fallback.artist),
                metadata?.year,
                null,
                folderCovers[folder]?.document?.uri?.toString(),
                metadata?.embeddedArtwork,
            ))
            tracks += TrackEntity(
                relativePath = discovered.relativePath,
                documentUri = discovered.document.uri.toString(),
                albumId = albumId,
                title = MetadataFallbacks.preferred(metadata?.title, fallback.title),
                artist = MetadataFallbacks.preferred(metadata?.artist, fallback.artist),
                album = MetadataFallbacks.preferred(metadata?.album, fallback.album),
                durationMs = metadata?.durationMs ?: 0,
                trackNumber = metadata?.trackNumber ?: fallback.trackNumber,
                discNumber = metadata?.discNumber,
                status = status.name,
                lastSeenScanEpochMillis = scanEpochMillis,
            )
        }
        return ScanResult(albums.values.toList(), tracks, issues, skipped, unsupported)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
