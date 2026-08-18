package ca.stewark.nocturnel.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `favorite_tracks` (`relativePath` TEXT NOT NULL, `favoritedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`relativePath`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `favorite_albums` (`albumId` TEXT NOT NULL, `favoritedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`albumId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `track_play_stats` (`relativePath` TEXT NOT NULL, `playCount` INTEGER NOT NULL, `lastPlayedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`relativePath`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `play_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `qualificationId` TEXT NOT NULL, `relativePath` TEXT NOT NULL, `playedAtEpochMillis` INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_play_history_qualificationId` ON `play_history` (`qualificationId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_history_relativePath` ON `play_history` (`relativePath`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_history_playedAtEpochMillis` ON `play_history` (`playedAtEpochMillis`)")
    }
}
