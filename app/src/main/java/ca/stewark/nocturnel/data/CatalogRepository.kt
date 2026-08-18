package ca.stewark.nocturnel.data

import ca.stewark.nocturnel.data.dao.LibraryDao
import ca.stewark.nocturnel.data.entity.LibrarySourceEntity
import ca.stewark.nocturnel.data.entity.ScanIssueEntity
import ca.stewark.nocturnel.data.entity.ScanReportEntity
import ca.stewark.nocturnel.library.LibraryScanner
import ca.stewark.nocturnel.library.ScanOutcome
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

    suspend fun rescan(cancelled: () -> Boolean = { false }, onProgress: (Int) -> Unit = {}): ScanReport = withContext(Dispatchers.IO) {
        val source = requireNotNull(dao.librarySource()) { "Choose a music folder first." }
        if (!scanner.canAccess(source.treeUri)) {
            markAccessLost()
            throw LibraryAccessLostException()
        }
        val now = System.currentTimeMillis()
        val result = scanner.scan(source.treeUri, now, cancelled, onProgress)
        when (result.outcome) {
            ScanOutcome.ACCESS_LOST -> {
                markAccessLost()
                throw LibraryAccessLostException()
            }
            ScanOutcome.CANCELLED -> throw ScanCancelledException()
            ScanOutcome.COMPLETED -> Unit
        }
        val existing = dao.allTracks()
        val counts = ScanReconciler.count(existing.map(::fingerprint), result.tracks.map(::fingerprint))
        val report = ScanReport(now, counts.added, counts.changed, counts.missing, result.skipped, result.unsupported, result.issues)
        dao.replaceCompletedScan(
            result.albums,
            result.tracks,
            ScanReportEntity(now, report.added, report.changed, report.missing, report.skipped, report.unsupported),
            report.issues.map { ScanIssueEntity(now, it.relativePath, it.message) },
        )
        dao.saveLibrarySource(LibrarySourceEntity(0, source.treeUri, source.displayName, now, false))
        report
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
