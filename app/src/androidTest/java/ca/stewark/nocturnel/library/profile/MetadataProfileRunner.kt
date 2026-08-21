package ca.stewark.nocturnel.library.profile

import ca.stewark.nocturnel.library.DiscoveredDocument
import ca.stewark.nocturnel.library.MediaMetadataReader
import ca.stewark.nocturnel.library.ReadMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class ProfiledMetadata(
    val documentUri: String,
    val result: Result<ReadMetadata>,
)

class MetadataProfileRunner(private val reader: MediaMetadataReader) {
    suspend fun readTags(documents: List<DiscoveredDocument>, parallelism: Int): List<ProfiledMetadata> = coroutineScope {
        require(parallelism > 0)
        val permits = Semaphore(parallelism)
        documents.mapIndexed { index, document ->
            async(Dispatchers.IO) {
                permits.withPermit {
                    index to ProfiledMetadata(document.documentUri, reader.readTags(document.documentUri))
                }
            }
        }.awaitAll().sortedBy { it.first }.map { it.second }
    }
}
