package ca.stewark.nocturnel.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ca.stewark.nocturnel.NocturneLApplication
import ca.stewark.nocturnel.data.CatalogRepository
import ca.stewark.nocturnel.data.LibraryAccessLostException
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.playback.SharedPreferencesPlaybackStateRepository
import ca.stewark.nocturnel.library.model.LibrarySource
import ca.stewark.nocturnel.library.model.ScanReport
import ca.stewark.nocturnel.library.ScanProgress
import ca.stewark.nocturnel.ui.components.NoticeSeverity
import ca.stewark.nocturnel.ui.components.TransientNoticeState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RescanUiState(val progress: ScanProgress? = null, val report: ScanReport? = null) {
    val running: Boolean get() = progress != null
}
data class PendingSourceChange(val uri: Uri, val displayName: String)

class LibrarySourceViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NocturneLApplication
    private val catalog = CatalogRepository(app.database, app.scanner)
    private val playbackStateRepository = SharedPreferencesPlaybackStateRepository(application)
    val albums: StateFlow<List<AlbumEntity>> = app.database.libraryDao().albums().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playableTracks = app.database.libraryDao().playableTracks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var source: LibrarySource? by mutableStateOf(null)
        private set
    var scanState: RescanUiState by mutableStateOf(RescanUiState())
        private set
    val notices = TransientNoticeState(viewModelScope)
    private var scanJob: Job? = null
    var pendingSourceChange: PendingSourceChange? by mutableStateOf(null)
        private set

    init {
        viewModelScope.launch {
            source = catalog.source()
            if (source != null && !app.treeAccess.canRead(source!!.treeUri)) {
                catalog.markAccessLost()
                source = catalog.source()
                notices.persistent("Access to the selected music folder was lost. Choose the folder again in settings.")
            }
        }
    }

    fun selectFolder(uri: Uri) = viewModelScope.launch {
        runCatching {
            app.treeAccess.persist(uri)
            startSelectedSourceScan(uri.toString(), app.treeAccess.displayName(uri), source?.treeUri != uri.toString())
        }.onFailure { notices.persistent("Could not retain access to that folder.") }
    }

    fun requestFolder(uri: Uri) {
        val displayName = runCatching { app.treeAccess.displayName(uri) }.getOrDefault("SELECTED FOLDER")
        if (source != null && source?.treeUri != uri.toString()) {
            pendingSourceChange = PendingSourceChange(uri, displayName)
        } else {
            selectFolder(uri)
        }
    }

    fun confirmSourceChange() {
        val pending = pendingSourceChange ?: return
        pendingSourceChange = null
        selectFolder(pending.uri)
    }

    fun cancelSourceChange() { pendingSourceChange = null }

    fun rescan() {
        if (scanState.running) return
        scanJob = viewModelScope.launch {
            scanState = RescanUiState(progress = ScanProgress.Discovering)
            val runningContext = currentCoroutineContext()
            try {
                val report = catalog.rescan(
                    cancelled = { !runningContext.isActive },
                    onProgress = { scanState = scanState.copy(progress = it) },
                )
                source = catalog.source()
                scanState = RescanUiState(report = report)
                notices.info("Rescan complete")
            } catch (error: LibraryAccessLostException) {
                source = catalog.source()
                scanState = RescanUiState()
                notices.persistent(error.message ?: "Access to the selected music folder was lost.")
            } catch (error: Exception) {
                if (runningContext.isActive) {
                    scanState = RescanUiState()
                    notices.persistent(error.message ?: "Rescan failed")
                }
            }
        }
    }

    fun cancelRescan() {
        if (!scanState.running) return
        scanJob?.cancel()
        scanJob = null
        scanState = RescanUiState()
        notices.persistent("Rescan cancelled; the previous catalog was preserved.", NoticeSeverity.WARNING)
    }

    fun tracks(albumId: String) = app.database.libraryDao().tracksForAlbum(albumId)
    fun setManualArtwork(albumId: String, uri: Uri?) = viewModelScope.launch {
        try {
            uri?.let {
                getApplication<android.app.Application>().contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            app.database.libraryDao().updateManualArtwork(albumId, uri?.toString())
        } catch (_: SecurityException) {
            notices.persistent("Could not retain access to that cover image.")
        }
    }

    private fun startSelectedSourceScan(treeUri: String, displayName: String, changed: Boolean) {
        if (scanState.running) return
        scanJob = viewModelScope.launch {
            scanState = RescanUiState(progress = ScanProgress.Discovering)
            val runningContext = currentCoroutineContext()
            try {
                val report = catalog.scanSelectedSource(treeUri, displayName, { !runningContext.isActive }) {
                    scanState = scanState.copy(progress = it)
                }
                if (changed) playbackStateRepository.clear()
                source = catalog.source()
                scanState = RescanUiState(report = report)
                notices.info("Rescan complete")
            } catch (error: LibraryAccessLostException) {
                scanState = RescanUiState()
                notices.persistent(error.message ?: "Access to the selected music folder was lost.")
            } catch (error: Exception) {
                if (runningContext.isActive) {
                    scanState = RescanUiState()
                    notices.persistent(error.message ?: "Rescan failed")
                }
            }
        }
    }
}
