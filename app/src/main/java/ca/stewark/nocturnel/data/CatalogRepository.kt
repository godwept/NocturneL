package ca.stewark.nocturnel.data

import ca.stewark.nocturnel.data.dao.LibraryDao
import ca.stewark.nocturnel.data.entity.LibrarySourceEntity
import ca.stewark.nocturnel.data.entity.ScanIssueEntity
import ca.stewark.nocturnel.data.entity.ScanReportEntity
import ca.stewark.nocturnel.library.LibraryScanner
import ca.stewark.nocturnel.library.ScanOutcome
import ca.stewark.nocturnel.library.ScanProgress
import ca.stewark.nocturnel.library.model.LibrarySource
import ca.stewark.nocturnel.library.model.ScanReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibraryAccessLostException : IllegalStateException("Access to the selected music folder was lost.")
class ScanCancelledException : IllegalStateException("Library scan was cancelled.")

class CatalogRepository(private val database: NocturneLDatabase, private val scanner: LibraryScanner) {
    private val dao: LibraryDao = database.libraryDao()
    suspend fun source(): LibrarySource? = dao.librarySource()?.let { LibrarySource(it.treeUri, it.displayName, it.lastScanEpochMillis, it.accessLost) }

    suspend fun saveSource(treeUri: String, displayName: String): Boolean {
        val previous = dao.librarySource()
        val changed = previous != null && previous.treeUri != treeUri
        database.replaceLibrarySource(
            LibrarySourceEntity(treeUri = treeUri, displayName = displayName, lastScanEpochMillis = null, accessLost = false),
            sourceChanged = changed,
        )
        return changed
    }

    suspend fun markAccessLost() {
        dao.librarySource()?.let { dao.saveLibrarySource(it.copy(accessLost = true)) }
    }

    suspend fun rescan(
        cancelled: () -> Boolean = { false },
        onProgress: (ScanProgress) -> Unit = {},
    ): ScanReport = withContext(Dispatchers.IO) {
        val source = requireNotNull(dao.librarySource()) { "Choose a music folder first." }
        scan(source, sourceChanged = false, cancelled, onProgress)
    }

    suspend fun scanSelectedSource(
        treeUri: String,
        displayName: String,
        cancelled: () -> Boolean = { false },
        onProgress: (ScanProgress) -> Unit = {},
    ): ScanReport = withContext(Dispatchers.IO) {
        val previous = dao.librarySource()
        scan(
            LibrarySourceEntity(treeUri = treeUri, displayName = displayName, lastScanEpochMillis = null, accessLost = false),
            sourceChanged = previous?.treeUri != treeUri,
            cancelled = cancelled,
            onProgress = onProgress,
        )
    }

    private suspend fun scan(
        source: LibrarySourceEntity,
        sourceChanged: Boolean,
        cancelled: () -> Boolean,
        onProgress: (ScanProgress) -> Unit,
    ): ScanReport {
        if (!scanner.canAccess(source.treeUri)) {
            if (!sourceChanged) markAccessLost()
            throw LibraryAccessLostException()
        }
        val now = System.currentTimeMillis()
        val result = scanner.scan(source.treeUri, now, cancelled, onProgress)
        when (result.outcome) {
            ScanOutcome.ACCESS_LOST -> {
                if (!sourceChanged) markAccessLost()
                throw LibraryAccessLostException()
            }
            ScanOutcome.CANCELLED -> throw ScanCancelledException()
            ScanOutcome.COMPLETED -> Unit
        }
        val existing = if (sourceChanged) emptyList() else dao.allTracks()
        val counts = ScanReconciler.count(existing.map(::fingerprint), result.tracks.map(::fingerprint))
        val report = ScanReport(now, counts.added, counts.changed, counts.missing, result.skipped, result.unsupported, result.issues)
        val entity = ScanReportEntity(now, report.added, report.changed, report.missing, report.skipped, report.unsupported)
        if (sourceChanged) {
            database.replaceSourceAndCompletedScan(source.copy(lastScanEpochMillis = now), result.albums, result.tracks, entity, report.issues.map { ScanIssueEntity(now, it.relativePath, it.message) })
        } else {
            dao.replaceCompletedScan(result.albums, result.tracks, entity, report.issues.map { ScanIssueEntity(now, it.relativePath, it.message) })
            dao.saveLibrarySource(source.copy(lastScanEpochMillis = now))
        }
        return report
    }

    private fun fingerprint(track: ca.stewark.nocturnel.data.entity.TrackEntity): TrackFingerprint =
        TrackFingerprint(
            track.relativePath,
            listOf(
                track.documentUri,
                track.albumId,
                track.title,
                track.artist,
                track.album,
                track.durationMs,
                track.trackNumber,
                track.discNumber,
                track.status,
            ).joinToString("\u0000"),
        )
}
