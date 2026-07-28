package ca.stewark.nocturnel.ui.playlist

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ca.stewark.nocturnel.NocturneLApplication
import ca.stewark.nocturnel.playlist.M3u8Codec
import ca.stewark.nocturnel.playlist.M3u8DocumentService
import ca.stewark.nocturnel.playlist.PlaylistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ca.stewark.nocturnel.data.entity.TrackEntity

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NocturneLApplication
    private val dao = app.database.libraryDao()
    private val repository = PlaylistRepository(dao)
    private val documentService = M3u8DocumentService(application.contentResolver)
    val playlists = dao.playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var message: String? by mutableStateOf(null)
        private set

    fun create(name: String = "New playlist") = viewModelScope.launch {
        repository.create(name)
        message = "Playlist created"
    }

    fun rename(id: Long, name: String) = viewModelScope.launch { repository.rename(id, name); message = "Playlist renamed" }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id); message = "Playlist deleted" }

    fun import(uri: Uri) = viewModelScope.launch {
        runCatching {
            val known = dao.allTracks().map { it.relativePath }.toSet()
            val parsed = M3u8Codec.parse(documentService.read(uri), known)
            val id = repository.create(uri.lastPathSegment?.substringBeforeLast('.') ?: "Imported playlist")
            repository.replaceEntries(id, parsed.paths)
            "Imported ${parsed.paths.size} track(s); skipped ${parsed.skipped.size}."
        }.onSuccess { message = it }.onFailure { message = "Playlist import failed" }
    }

    fun export(playlistId: Long, uri: Uri) = viewModelScope.launch {
        runCatching { documentService.write(uri, M3u8Codec.encode(repository.paths(playlistId))) }
            .onSuccess { message = "Playlist exported" }
            .onFailure { message = "Playlist export failed" }
    }
    suspend fun playableTracks(playlistId: Long): List<TrackEntity> = repository.playableTracks(playlistId)
}
