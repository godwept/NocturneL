package ca.stewark.nocturnel.ui.listening

import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.data.model.ListeningHistoryRow
import ca.stewark.nocturnel.playback.PlaybackUiState

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
) {
    val previewFavoriteAlbums: List<AlbumEntity> get() = favoriteAlbums.take(3)
    val previewFavoriteTracks: List<TrackEntity> get() = favoriteTracks.take(3)
    val previewRecentTracks: List<ListeningHistoryRow> get() = recentTracks.take(5)
}

data class ResumeUiState(
    val title: String,
    val artist: String,
    val positionMs: Long,
    val durationMs: Long,
    val enabled: Boolean,
)

fun resumeState(playback: PlaybackUiState, sourceAccessible: Boolean): ResumeUiState? {
    if (playback.currentPath == null || playback.meaningfulProgressMs < 10_000 || playback.playing || playback.completed) return null
    return ResumeUiState(
        title = playback.title ?: playback.currentPath.substringAfterLast('/'),
        artist = playback.artist.orEmpty(),
        positionMs = playback.positionMs,
        durationMs = playback.durationMs,
        enabled = sourceAccessible,
    )
}
