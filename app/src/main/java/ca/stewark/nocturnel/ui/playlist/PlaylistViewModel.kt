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
import ca.stewark.nocturnel.playlist.PlaylistDocumentService
import ca.stewark.nocturnel.playlist.PlaylistRepository
import ca.stewark.nocturnel.playlist.AppendAlbumResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ca.stewark.nocturnel.data.entity.TrackEntity

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NocturneLApplication
    private val dao = app.database.libraryDao()
    private val repository = PlaylistRepository(dao)
    private val documentService = PlaylistDocumentService(application.contentResolver)
    val playlists = dao.playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var message: String? by mutableStateOf(null)
        private set
    private val _detail = MutableStateFlow<PlaylistDetailState?>(null)
    val detail: StateFlow<PlaylistDetailState?> = _detail.asStateFlow()
    private val _albumPlaylistState = MutableStateFlow<AlbumPlaylistUiState>(AlbumPlaylistUiState.Idle)
    val albumPlaylistState: StateFlow<AlbumPlaylistUiState> = _albumPlaylistState.asStateFlow()
    private val albumPlaylistCommand = AlbumPlaylistCommand(
        appendAlbum = repository::appendAlbum,
        createPlaylist = repository::create,
    )
    private val importCommand = PlaylistImportCommand(
        existingNames = { dao.allPlaylists().map { it.name } },
        knownPaths = { dao.allTracks().map { it.relativePath }.toSet() },
        createWithEntries = { name, paths -> repository.createWithEntries(name, paths); Unit },
    )
    private val exportCommand = PlaylistExportCommand(dao::allPlaylists, repository::paths)

    fun create(name: String = "New playlist") = viewModelScope.launch {
        repository.create(name)
        message = "Playlist created"
    }

    fun open(id: Long) = viewModelScope.launch { refresh(id) }
    fun close() { _detail.value = null }

    fun rename(id: Long, name: String) = viewModelScope.launch {
        repository.rename(id, name)
        message = "Playlist renamed"
        if (_detail.value?.playlist?.id == id) refresh(id)
    }
    fun delete(id: Long) = viewModelScope.launch {
        repository.delete(id)
        if (_detail.value?.playlist?.id == id) _detail.value = null
        message = "Playlist deleted"
    }
    fun add(id: Long, path: String) = viewModelScope.launch { repository.add(id, path); refresh(id) }
    fun remove(id: Long, index: Int) = viewModelScope.launch { repository.removeAt(id, index); refresh(id) }
    fun move(id: Long, from: Int, to: Int) = viewModelScope.launch { repository.move(id, from, to); refresh(id) }

    fun addAlbum(playlistId: Long, playlistName: String, tracks: List<TrackEntity>) = viewModelScope.launch {
        _albumPlaylistState.value = AlbumPlaylistUiState.Working
        _albumPlaylistState.value = albumPlaylistCommand.add(playlistId, playlistName, tracks)
    }

    fun createAndAddAlbum(name: String, tracks: List<TrackEntity>) = viewModelScope.launch {
        _albumPlaylistState.value = AlbumPlaylistUiState.Working
        _albumPlaylistState.value = albumPlaylistCommand.createAndAdd(name, tracks)
    }

    fun clearAlbumPlaylistState() {
        _albumPlaylistState.value = AlbumPlaylistUiState.Idle
    }

    fun import(uri: Uri) = viewModelScope.launch {
        runCatching { importCommand.import(documentService.readImport(uri)).message }
            .onSuccess { message = it }
            .onFailure { message = PlaylistTransferMessages.IMPORT_FAILED }
    }

    fun export(playlistId: Long, uri: Uri) = viewModelScope.launch {
        runCatching { documentService.writeM3u8(uri, M3u8Codec.encode(repository.paths(playlistId))) }
            .onSuccess { message = PlaylistTransferMessages.PLAYLIST_EXPORTED }
            .onFailure { message = PlaylistTransferMessages.EXPORT_FAILED }
    }

    fun exportAll(uri: Uri) = viewModelScope.launch {
        runCatching {
            val playlists = exportCommand.collect()
            documentService.writeBundle(uri, playlists)
            PlaylistExportSummary(playlists.size).message
        }.onSuccess { message = it }.onFailure { message = PlaylistTransferMessages.EXPORT_FAILED }
    }

    fun importCancelled() { message = PlaylistTransferMessages.IMPORT_CANCELLED }
    fun exportCancelled() { message = PlaylistTransferMessages.EXPORT_CANCELLED }
    suspend fun playableTracks(playlistId: Long): List<TrackEntity> = repository.playableTracks(playlistId)

    private suspend fun refresh(id: Long) {
        val playlist = dao.playlist(id) ?: run { _detail.value = null; return }
        _detail.value = playlistDetailState(playlist, dao.playlistEntryRows(id), dao.allTracks())
    }
}

internal class AlbumPlaylistCommand(
    private val appendAlbum: suspend (Long, List<String>) -> AppendAlbumResult,
    private val createPlaylist: suspend (String) -> Long,
) {
    suspend fun add(playlistId: Long, playlistName: String, tracks: List<TrackEntity>): AlbumPlaylistUiState =
        runCatching {
            appendAlbum(playlistId, tracks.playablePaths())
        }.fold(
            onSuccess = { albumAppendResultState(playlistName, it) },
            onFailure = ::albumAppendFailureState,
        )

    suspend fun createAndAdd(name: String, tracks: List<TrackEntity>): AlbumPlaylistUiState {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return AlbumPlaylistUiState.Error("PLAYLIST NAME IS REQUIRED")
        return runCatching {
            val playlistId = createPlaylist(trimmed)
            appendAlbum(playlistId, tracks.playablePaths())
        }.fold(
            onSuccess = { albumAppendResultState(trimmed, it) },
            onFailure = { AlbumPlaylistUiState.Error("COULD NOT ADD ALBUM TO PLAYLIST") },
        )
    }

    private fun List<TrackEntity>.playablePaths(): List<String> =
        filter { it.status == "PLAYABLE" }.map { it.relativePath }
}
