package ca.stewark.nocturnel.ui.listening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import ca.stewark.nocturnel.NocturneLApplication
import ca.stewark.nocturnel.data.ListeningRepository
import ca.stewark.nocturnel.data.ListeningStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class FavoriteData(
    val trackPaths: List<String>,
    val albumIds: List<String>,
    val tracks: List<ca.stewark.nocturnel.data.entity.TrackEntity>,
    val albums: List<ca.stewark.nocturnel.data.entity.AlbumEntity>,
)

private data class ActivityData(
    val trackCounts: Map<String, Long>,
    val albumCounts: Map<String, Long>,
    val history: List<ca.stewark.nocturnel.data.model.ListeningHistoryRow>,
    val recent: List<ca.stewark.nocturnel.data.model.ListeningHistoryRow>,
)

class ListeningViewModel(private val store: ListeningStore) : ViewModel() {
    private val message = MutableStateFlow<String?>(null)
    private val optimisticTracks = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val optimisticAlbums = MutableStateFlow<Map<String, Boolean>>(emptyMap())

    private val favorites = combine(
        store.favoriteTrackPaths,
        store.favoriteAlbumIds,
        store.favoriteTracks,
        store.favoriteAlbums,
    ) { paths, albumIds, tracks, albums -> FavoriteData(paths, albumIds, tracks, albums) }

    private val activity = combine(
        combine(store.trackPlayCounts, store.albumPlayCounts) { tracks, albums ->
            tracks.associate { it.relativePath to it.playCount } to albums.associate { it.albumId to it.playCount }
        },
        store.history,
        store.recentDistinct,
    ) { counts, history, recent -> ActivityData(counts.first, counts.second, history, recent) }

    val state: StateFlow<ListeningUiState> = combine(
        favorites,
        activity,
        optimisticTracks,
        optimisticAlbums,
        message,
    ) { favorite, listening, pendingTracks, pendingAlbums, notice ->
        val trackPaths = favorite.trackPaths.toMutableSet().apply {
            pendingTracks.forEach { (key, value) -> if (value) add(key) else remove(key) }
        }
        val albumIds = favorite.albumIds.toMutableSet().apply {
            pendingAlbums.forEach { (key, value) -> if (value) add(key) else remove(key) }
        }
        ListeningUiState(
            favoriteTrackPaths = trackPaths,
            favoriteAlbumIds = albumIds,
            favoriteTracks = favorite.tracks,
            favoriteAlbums = favorite.albums,
            trackPlayCounts = listening.trackCounts,
            albumPlayCounts = listening.albumCounts,
            history = listening.history,
            recentTracks = listening.recent,
            message = notice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListeningUiState())

    fun toggleTrack(path: String) {
        val desired = path !in state.value.favoriteTrackPaths
        optimisticTracks.value = optimisticTracks.value + (path to desired)
        viewModelScope.launch {
            runCatching { store.toggleTrack(path) }
                .onFailure { message.value = "COULD NOT UPDATE TRACK FAVORITE" }
            optimisticTracks.value = optimisticTracks.value - path
        }
    }

    fun toggleAlbum(albumId: String) {
        val desired = albumId !in state.value.favoriteAlbumIds
        optimisticAlbums.value = optimisticAlbums.value + (albumId to desired)
        viewModelScope.launch {
            runCatching { store.toggleAlbum(albumId) }
                .onFailure { message.value = "COULD NOT UPDATE ALBUM FAVORITE" }
            optimisticAlbums.value = optimisticAlbums.value - albumId
        }
    }

    fun clearHistoryAndCounts() = viewModelScope.launch {
        runCatching { store.clearHistoryAndCounts() }
            .onSuccess { message.value = "HISTORY AND PLAY COUNTS CLEARED" }
            .onFailure { message.value = "COULD NOT CLEAR LISTENING DATA" }
    }

    class Factory(private val app: NocturneLApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            ListeningViewModel(ListeningRepository(app.database.listeningDao())) as T
    }
}
