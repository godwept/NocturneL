package ca.stewark.nocturnel.library.profile

import android.os.SystemClock
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ca.stewark.nocturnel.NocturneLApplication
import ca.stewark.nocturnel.data.NocturneLDatabase
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.library.AndroidMediaMetadataReader
import ca.stewark.nocturnel.library.DiscoveredDocument
import ca.stewark.nocturnel.library.DocumentFileEnumerator
import ca.stewark.nocturnel.library.SupportedAudioFormats
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryScanPerformanceProfileTest {
    @Test fun candidatesProduceEquivalentResultsAndReportStageMedians() = runTest {
        assumeTrue(InstrumentationRegistry.getArguments().getString("scanProfile") == "true")
        val app = ApplicationProvider.getApplicationContext<NocturneLApplication>()
        val source = requireNotNull(app.database.libraryDao().librarySource()) {
            "Select the real music folder in the debug app before running scan profiling."
        }
        val baseline = DocumentFileEnumerator(app.treeAccess)
        val direct = DocumentsContractProfileEnumerator(app.contentResolver)

        baseline.enumerate(source.treeUri)
        direct.enumerate(source.treeUri)
        val baselineRuns = measureFive { baseline.enumerate(source.treeUri) }
        val directRuns = measureFive { direct.enumerate(source.treeUri) }
        val baselineDocuments = baseline.enumerate(source.treeUri).normalized()
        val directDocuments = direct.enumerate(source.treeUri).normalized()
        assertEquals(baselineDocuments, directDocuments)

        val audio = baselineDocuments.filter { SupportedAudioFormats.isCandidateAudioFile(it.relativePath) }
        val metadata = MetadataProfileRunner(AndroidMediaMetadataReader(app))
        metadata.readTags(audio, 1)
        val tagRuns = listOf(1, 2, 4).associateWith { limit ->
            measureFiveSuspend { metadata.readTags(audio, limit) }
        }
        val baselineTags = metadata.readTags(audio, 1).map { it.result.getOrNull() }
        for (limit in listOf(2, 4)) {
            assertEquals(baselineTags, metadata.readTags(audio, limit).map { it.result.getOrNull() })
        }

        val artworkReader = AndroidMediaMetadataReader(app)
        val artworkRuns = measureFive {
            for (albumDocuments in audio.groupBy { it.relativePath.substringBeforeLast('/', "") }.values) {
                for (document in albumDocuments) {
                    val artwork = artworkReader.readArtwork(document.documentUri).getOrNull()
                    if (artwork != null && artwork.isNotEmpty()) break
                }
            }
        }
        val persistenceRuns = profilePersistence(app, audio)
        println("[SCAN_PROFILE] files=${baselineDocuments.size} audio=${audio.size}")
        println("[SCAN_PROFILE] discovery_document_file_ms=$baselineRuns median=${baselineRuns.median()}")
        println("[SCAN_PROFILE] discovery_documents_contract_ms=$directRuns median=${directRuns.median()}")
        tagRuns.forEach { (limit, runs) -> println("[SCAN_PROFILE] tags_parallelism_${limit}_ms=$runs median=${runs.median()}") }
        println("[SCAN_PROFILE] artwork_first_usable_per_album_ms=$artworkRuns median=${artworkRuns.median()}")
        println("[SCAN_PROFILE] persistence_ms=$persistenceRuns median=${persistenceRuns.median()}")
    }

    private fun List<DiscoveredDocument>.normalized() = sortedBy { it.relativePath }

    private fun measureFive(block: () -> Unit): List<Long> = List(5) {
        val start = SystemClock.elapsedRealtimeNanos()
        block()
        (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000
    }

    private suspend fun measureFiveSuspend(block: suspend () -> Unit): List<Long> = List(5) {
        val start = SystemClock.elapsedRealtimeNanos()
        block()
        (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000
    }

    private suspend fun profilePersistence(
        app: NocturneLApplication,
        documents: List<DiscoveredDocument>,
    ): List<Long> {
        val database = Room.inMemoryDatabaseBuilder(app, NocturneLDatabase::class.java).build()
        return try {
            val tracks = documents.mapIndexed { index, document ->
                TrackEntity(
                    relativePath = document.relativePath,
                    documentUri = document.documentUri,
                    albumId = "profile",
                    title = document.displayName,
                    artist = "Profile",
                    album = "Profile",
                    durationMs = 0,
                    trackNumber = index,
                    discNumber = null,
                    status = "PLAYABLE",
                    lastSeenScanEpochMillis = 1,
                    fileSizeBytes = document.fileSizeBytes,
                    lastModifiedEpochMillis = document.lastModifiedEpochMillis,
                )
            }
            measureFiveSuspend { database.libraryDao().saveTracks(tracks) }
        } finally {
            database.close()
        }
    }

    private fun List<Long>.median(): Long = sorted()[size / 2]
}
