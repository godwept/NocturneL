package ca.stewark.nocturnel.data.model

data class TrackPlayCountRow(val relativePath: String, val playCount: Long)

data class AlbumPlayCountRow(val albumId: String, val playCount: Long)

data class ListeningHistoryRow(
    val id: Long,
    val qualificationId: String,
    val relativePath: String,
    val playedAtEpochMillis: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumId: String?,
    val durationMs: Long?,
    val status: String?,
    val documentUri: String?,
)
