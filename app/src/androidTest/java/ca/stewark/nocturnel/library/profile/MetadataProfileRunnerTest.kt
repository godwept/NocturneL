package ca.stewark.nocturnel.library.profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import ca.stewark.nocturnel.library.DiscoveredDocument
import ca.stewark.nocturnel.library.MediaMetadataReader
import ca.stewark.nocturnel.library.ReadMetadata
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MetadataProfileRunnerTest {
    @Test fun limitsOneTwoAndFourReturnResultsInInputOrder() = runTest {
        val documents = (1..5).map { DiscoveredDocument("$it.mp3", "uri:$it", "$it.mp3", 1, 1) }
        val runner = MetadataProfileRunner(FakeReader())

        for (limit in listOf(1, 2, 4)) {
            assertEquals(documents.map { it.documentUri }, runner.readTags(documents, limit).map { it.documentUri })
        }
    }

    private class FakeReader : MediaMetadataReader {
        override fun readTags(documentUri: String) = Result.success(ReadMetadata(documentUri, null, null, null, null, null, 1))
        override fun readArtwork(documentUri: String): Result<ByteArray?> = Result.success(null)
    }
}
