package ca.stewark.nocturnel.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import ca.stewark.nocturnel.data.dao.LibraryDao
import ca.stewark.nocturnel.data.dao.ListeningDao
import ca.stewark.nocturnel.data.entity.FavoriteAlbumEntity
import ca.stewark.nocturnel.data.entity.FavoriteTrackEntity
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.LibrarySourceEntity
import ca.stewark.nocturnel.data.entity.PlaybackPreferenceEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntryEntity
import ca.stewark.nocturnel.data.entity.ScanIssueEntity
import ca.stewark.nocturnel.data.entity.ScanReportEntity
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.data.entity.TrackPlayStatsEntity
import ca.stewark.nocturnel.data.entity.PlayHistoryEntity

@Database(
    entities = [LibrarySourceEntity::class, AlbumEntity::class, TrackEntity::class, PlaylistEntity::class, PlaylistEntryEntity::class, ScanReportEntity::class, ScanIssueEntity::class, PlaybackPreferenceEntity::class, FavoriteTrackEntity::class, FavoriteAlbumEntity::class, TrackPlayStatsEntity::class, PlayHistoryEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class NocturneLDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun listeningDao(): ListeningDao

    suspend fun replaceLibrarySource(source: LibrarySourceEntity, sourceChanged: Boolean) = withTransaction {
        libraryDao().selectLibrarySource(source, sourceChanged)
        if (sourceChanged) listeningDao().clearAllListeningData()
    }
}
