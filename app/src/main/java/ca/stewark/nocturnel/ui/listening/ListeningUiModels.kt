package ca.stewark.nocturnel.ui.listening

import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.data.model.ListeningHistoryRow

data class ListeningUiState(
    val favoriteTrackPaths: Set<String> = emptySet(),
    val favoriteAlbumIds: Set<String> = emptySet(),
    val favoriteTracks: List<TrackEntity> = emptyList(),
    val favoriteAlbums: List<AlbumEntity> = emptyList(),
    val trackPlayCounts: Map<String, Long> = emptyMap(),
    val albumPlayCounts: Map<String, Long> = emptyMap(),
    val history: List<ListeningHistoryRow> = emptyList(),
    val recentTracks: List<ListeningHistoryRow> = emptyList(),
    val message: String? = null,
)
