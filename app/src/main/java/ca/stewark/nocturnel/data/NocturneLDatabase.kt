package ca.stewark.nocturnel.data

import androidx.room.Database
import androidx.room.RoomDatabase
import ca.stewark.nocturnel.data.dao.LibraryDao
import ca.stewark.nocturnel.data.entity.AlbumEntity
import ca.stewark.nocturnel.data.entity.LibrarySourceEntity
import ca.stewark.nocturnel.data.entity.PlaybackPreferenceEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.data.entity.PlaylistEntryEntity
import ca.stewark.nocturnel.data.entity.ScanIssueEntity
import ca.stewark.nocturnel.data.entity.ScanReportEntity
import ca.stewark.nocturnel.data.entity.TrackEntity

@Database(
    entities = [LibrarySourceEntity::class, AlbumEntity::class, TrackEntity::class, PlaylistEntity::class, PlaylistEntryEntity::class, ScanReportEntity::class, ScanIssueEntity::class, PlaybackPreferenceEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class NocturneLDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
