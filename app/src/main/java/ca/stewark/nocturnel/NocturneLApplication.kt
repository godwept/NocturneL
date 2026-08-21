package ca.stewark.nocturnel

import android.app.Application
import androidx.room.Room
import ca.stewark.nocturnel.data.NocturneLDatabase
import ca.stewark.nocturnel.data.MIGRATION_1_2
import ca.stewark.nocturnel.data.MIGRATION_2_3
import ca.stewark.nocturnel.library.AndroidMediaMetadataReader
import ca.stewark.nocturnel.library.DocumentFileEnumerator
import ca.stewark.nocturnel.library.LibraryScanner
import ca.stewark.nocturnel.library.LibraryTreeAccess
import ca.stewark.nocturnel.visualizer.AudioAnalysisRepository
import java.util.UUID

class NocturneLApplication : Application() {
    val playbackSessionId: String = UUID.randomUUID().toString()
    val audioAnalysis: AudioAnalysisRepository by lazy { AudioAnalysisRepository() }
    val database: NocturneLDatabase by lazy {
        Room.databaseBuilder(this, NocturneLDatabase::class.java, "nocturnel.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }
    val treeAccess: LibraryTreeAccess by lazy { LibraryTreeAccess(this) }
    val scanner: LibraryScanner by lazy { LibraryScanner(DocumentFileEnumerator(treeAccess), AndroidMediaMetadataReader(this)) }
}
