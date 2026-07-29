package ca.stewark.nocturnel.data.model

data class PlaylistEntryRow(
    val position: Int,
    val relativePath: String,
    val title: String?,
    val artist: String?,
    val durationMs: Long?,
    val trackStatus: String?,
)
