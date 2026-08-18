package ca.stewark.nocturnel.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val relativePath: String,
    val favoritedAtEpochMillis: Long,
)

@Entity(tableName = "favorite_albums")
data class FavoriteAlbumEntity(
    @PrimaryKey val albumId: String,
    val favoritedAtEpochMillis: Long,
)

@Entity(tableName = "track_play_stats")
data class TrackPlayStatsEntity(
    @PrimaryKey val relativePath: String,
    val playCount: Long,
    val lastPlayedAtEpochMillis: Long,
)

@Entity(
    tableName = "play_history",
    indices = [
        Index(value = ["qualificationId"], unique = true),
        Index(value = ["relativePath"]),
        Index(value = ["playedAtEpochMillis"]),
    ],
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val qualificationId: String,
    val relativePath: String,
    val playedAtEpochMillis: Long,
)
