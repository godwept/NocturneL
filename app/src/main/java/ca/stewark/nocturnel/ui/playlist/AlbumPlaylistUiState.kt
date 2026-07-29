package ca.stewark.nocturnel.ui.playlist

import ca.stewark.nocturnel.playlist.AppendAlbumResult
import ca.stewark.nocturnel.playlist.PlaylistNotFoundException

sealed interface AlbumPlaylistUiState {
    data object Idle : AlbumPlaylistUiState
    data object Working : AlbumPlaylistUiState
    data class Success(val message: String) : AlbumPlaylistUiState
    data class AlreadyPresent(val message: String) : AlbumPlaylistUiState
    data class Warning(val message: String) : AlbumPlaylistUiState
    data class Error(val message: String) : AlbumPlaylistUiState
}

fun albumAppendResultState(playlistName: String, result: AppendAlbumResult): AlbumPlaylistUiState =
    if (result.added == 0) {
        AlbumPlaylistUiState.AlreadyPresent("ALBUM ALREADY IN PLAYLIST")
    } else {
        val skipped = if (result.skipped > 0) " · SKIPPED ${result.skipped}" else ""
        AlbumPlaylistUiState.Success("ADDED ${result.added} TRACK(S) TO ${playlistName.uppercase()}$skipped")
    }

fun albumAppendFailureState(error: Throwable): AlbumPlaylistUiState =
    if (error is PlaylistNotFoundException) {
        AlbumPlaylistUiState.Warning("PLAYLIST NO LONGER EXISTS")
    } else {
        AlbumPlaylistUiState.Error("COULD NOT ADD ALBUM TO PLAYLIST")
    }
