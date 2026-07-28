package ca.stewark.nocturnel.data

import ca.stewark.nocturnel.data.dao.LibraryDao
import ca.stewark.nocturnel.data.entity.LibrarySourceEntity
import ca.stewark.nocturnel.data.entity.ScanIssueEntity
import ca.stewark.nocturnel.data.entity.ScanReportEntity
import ca.stewark.nocturnel.library.LibraryScanner
import ca.stewark.nocturnel.library.model.LibrarySource
import ca.stewark.nocturnel.library.model.ScanReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CatalogRepository(private val dao: LibraryDao, private val scanner: LibraryScanner) {
    suspend fun source(): LibrarySource? = dao.librarySource()?.let { LibrarySource(it.treeUri, it.displayName, it.lastScanEpochMillis, it.accessLost) }

    suspend fun saveSource(treeUri: String, displayName: String) {
        dao.saveLibrarySource(LibrarySourceEntity(treeUri = treeUri, displayName = displayName, lastScanEpochMillis = null, accessLost = false))
    }

    suspend fun markAccessLost() {
        dao.librarySource()?.let { dao.saveLibrarySource(it.copy(accessLost = true)) }
    }

    suspend fun rescan(cancelled: () -> Boolean = { false }, onProgress: (Int) -> Unit = {}): ScanReport = withContext(Dispatchers.IO) {
        val source = requireNotNull(dao.librarySource()) { "Choose a music folder first." }
        val now = System.currentTimeMillis()
        val result = scanner.scan(source.treeUri, now, cancelled, onProgress)
        if (cancelled()) return@withContext ScanReport(now, 0, 0, 0, result.skipped, result.unsupported, result.issues)
        val existingPaths = dao.allTracks().map { it.relativePath }.toSet()
        val added = result.tracks.count { it.relativePath !in existingPaths }
        val missing = ScanReconciler.missingPaths(existingPaths, result.tracks.map { it.relativePath }.toSet()).size
        val report = ScanReport(now, added, 0, missing, result.skipped, result.unsupported, result.issues)
        dao.replaceCompletedScan(
            result.albums,
            result.tracks,
            ScanReportEntity(now, report.added, report.changed, report.missing, report.skipped, report.unsupported),
            report.issues.map { ScanIssueEntity(now, it.relativePath, it.message) },
        )
        dao.saveLibrarySource(LibrarySourceEntity(0, source.treeUri, source.displayName, now, false))
        report
    }
}
