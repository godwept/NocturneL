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
import ca.stewark.nocturnel.library.model.LibrarySource
import ca.stewark.nocturnel.library.model.ScanReport
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RescanUiState(val running: Boolean = false, val progress: Int = 0, val report: ScanReport? = null, val message: String? = null)

class LibrarySourceViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NocturneLApplication
    private val catalog = CatalogRepository(app.database.libraryDao(), app.scanner)
    val albums: StateFlow<List<AlbumEntity>> = app.database.libraryDao().albums().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val playableTracks = app.database.libraryDao().playableTracks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var source: LibrarySource? by mutableStateOf(null)
        private set
    var scanState: RescanUiState by mutableStateOf(RescanUiState())
        private set
    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            source = catalog.source()
            if (source != null && !app.treeAccess.canRead(source!!.treeUri)) {
                catalog.markAccessLost()
                source = catalog.source()
                scanState = RescanUiState(message = "Access to the selected music folder was lost. Choose the folder again in settings.")
            }
        }
    }

    fun selectFolder(uri: Uri) = viewModelScope.launch {
        runCatching {
            app.treeAccess.persist(uri)
            catalog.saveSource(uri.toString(), app.treeAccess.displayName(uri))
            source = catalog.source()
            scanState = RescanUiState(message = "Folder selected. Choose RESCAN to index it.")
        }.onFailure { scanState = RescanUiState(message = "Could not retain access to that folder.") }
    }

    fun rescan() {
        if (scanState.running) return
        scanJob = viewModelScope.launch {
            scanState = RescanUiState(running = true)
            val runningContext = currentCoroutineContext()
            try {
                val report = catalog.rescan(
                    cancelled = { !runningContext.isActive },
                    onProgress = { scanState = scanState.copy(progress = it) },
                )
                source = catalog.source()
                scanState = RescanUiState(report = report, message = "Rescan complete")
            } catch (error: LibraryAccessLostException) {
                source = catalog.source()
                scanState = RescanUiState(message = error.message)
            } catch (error: Exception) {
                if (runningContext.isActive) scanState = RescanUiState(message = error.message ?: "Rescan failed")
            }
        }
    }

    fun cancelRescan() {
        if (!scanState.running) return
        scanJob?.cancel()
        scanJob = null
        scanState = RescanUiState(message = "Rescan cancelled; the previous catalog was preserved.")
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
            scanState = scanState.copy(message = "Could not retain access to that cover image.")
        }
    }
}
