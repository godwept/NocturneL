package ca.stewark.nocturnel.ui.playlist

import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.data.model.PlaylistEntryRow

data class PlaylistTrackRow(
    val position: Int,
    val relativePath: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val available: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
)

data class PlaylistDetailState(
    val playlist: PlaylistEntity,
    val entries: List<PlaylistTrackRow>,
    val availableTracks: List<TrackEntity>,
)

fun playlistDetailState(
    playlist: PlaylistEntity,
    rows: List<PlaylistEntryRow>,
    allTracks: List<TrackEntity>,
): PlaylistDetailState {
    val existing = rows.map { it.relativePath }.toSet()
    return PlaylistDetailState(
        playlist,
        rows.mapIndexed { index, row ->
            PlaylistTrackRow(
                row.position,
                row.relativePath,
                row.title ?: row.relativePath.substringAfterLast('/'),
                row.artist ?: "UNAVAILABLE",
                row.durationMs ?: 0,
                row.trackStatus == "PLAYABLE",
                index > 0,
                index < rows.lastIndex,
            )
        },
        allTracks.filter { it.status == "PLAYABLE" && it.relativePath !in existing },
    )
}
