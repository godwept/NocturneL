package ca.stewark.nocturnel

import android.app.Application
import androidx.room.Room
import ca.stewark.nocturnel.data.NocturneLDatabase
import ca.stewark.nocturnel.library.AndroidMediaMetadataReader
import ca.stewark.nocturnel.library.LibraryScanner
import ca.stewark.nocturnel.library.LibraryTreeAccess
import ca.stewark.nocturnel.visualizer.AudioAnalysisRepository

class NocturneLApplication : Application() {
    val audioAnalysis: AudioAnalysisRepository by lazy { AudioAnalysisRepository() }
    val database: NocturneLDatabase by lazy {
        Room.databaseBuilder(this, NocturneLDatabase::class.java, "nocturnel.db").build()
    }
    val treeAccess: LibraryTreeAccess by lazy { LibraryTreeAccess(this) }
    val scanner: LibraryScanner by lazy { LibraryScanner(treeAccess, AndroidMediaMetadataReader(this)) }
}
