package ca.stewark.nocturnel.library

import ca.stewark.nocturnel.library.model.TrackStatus
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryScannerTest {
    @Test fun enumerationAccessFailureReportsAccessLost() {
        val enumerator = object : LibraryDocumentEnumerator {
            override fun canAccess(treeUri: String) = true
            override fun enumerate(treeUri: String, cancelled: () -> Boolean): List<DiscoveredDocument> {
                throw LibraryEnumerationAccessException()
            }
        }

        val result = LibraryScanner(enumerator, FakeMetadataReader()).scan("tree", 1)

        assertEquals(ScanOutcome.ACCESS_LOST, result.outcome)
    }

    @Test fun baselineScanPreservesOrderProgressAndFallbackMetadata() {
        val documents = listOf(document("Artist/Album/01 - First.mp3", "uri:1"), document("notes.txt", "uri:notes"))
        val reader = FakeMetadataReader()
        val progress = mutableListOf<ScanProgress>()

        val result = scanner(documents, reader).scan("tree", 10, onProgress = progress::add)

        assertEquals(listOf("Artist/Album/01 - First.mp3"), result.tracks.map { it.relativePath })
        assertEquals("First", result.tracks.single().title)
        assertEquals(listOf(ScanProgress.Discovering, ScanProgress.Indexing(1, 2), ScanProgress.Indexing(2, 2)), progress)
    }

    @Test fun unchangedPlayableTrackSkipsTagsAndArtwork() {
        val documents = listOf(document("Artist/Album/01.mp3", "uri:1"))
        val reader = FakeMetadataReader(artwork = mutableMapOf("uri:1" to Result.success(byteArrayOf(7))))
        val scanner = scanner(documents, reader)
        val initial = scanner.scan("tree", 1)
        reader.clearCalls()

        val result = scanner.scan("tree", 10, existingCatalog = ExistingCatalogSnapshot.from(initial.albums, initial.tracks))

        assertTrue(reader.tagUris.isEmpty())
        assertTrue(reader.artworkUris.isEmpty())
        assertEquals(10, result.tracks.single().lastSeenScanEpochMillis)
        assertArrayEquals(byteArrayOf(7), result.albums.single().embeddedArtwork)
    }

    @Test fun changedUnknownAndPriorMetadataIssueTracksAreReread() {
        val reliable = document("Album/01.mp3", "uri:1")
        val reader = FakeMetadataReader()
        val scanner = scanner(listOf(reliable), reader)
        val initial = scanner.scan("tree", 1)

        reader.clearCalls()
        scanner(listOf(reliable.copy(fileSizeBytes = 43)), reader)
            .scan("tree", 2, existingCatalog = ExistingCatalogSnapshot.from(initial.albums, initial.tracks))
        assertEquals(listOf("uri:1"), reader.tagUris)

        reader.clearCalls()
        scanner(listOf(reliable.copy(lastModifiedEpochMillis = null)), reader)
            .scan("tree", 3, existingCatalog = ExistingCatalogSnapshot.from(initial.albums, initial.tracks))
        assertEquals(listOf("uri:1"), reader.tagUris)

        val issueTrack = initial.tracks.single().copy(status = TrackStatus.METADATA_ISSUE.name)
        reader.clearCalls()
        scanner.scan("tree", 4, existingCatalog = ExistingCatalogSnapshot.from(initial.albums, listOf(issueTrack)))
        assertEquals(listOf("uri:1"), reader.tagUris)
    }

    @Test fun metadataFailureStoresFingerprintAndIssue() {
        val reader = FakeMetadataReader(tags = mutableMapOf("uri:1" to Result.failure(IllegalStateException("bad"))))

        val result = scanner(listOf(document("Album/01.mp3", "uri:1")), reader).scan("tree", 1)

        assertEquals(TrackStatus.METADATA_ISSUE.name, result.tracks.single().status)
        assertEquals(42L, result.tracks.single().fileSizeBytes)
        assertEquals(1_000L, result.tracks.single().lastModifiedEpochMillis)
        assertEquals("Could not read media metadata", result.issues.single().message)
    }

    @Test fun dirtyAlbumStopsAfterFirstUsableArtwork() {
        val documents = listOf(
            document("Album/01.mp3", "uri:1"),
            document("Album/02.mp3", "uri:2"),
            document("Album/03.mp3", "uri:3"),
        )
        val reader = FakeMetadataReader(artwork = mutableMapOf(
            "uri:1" to Result.success(null),
            "uri:2" to Result.success(byteArrayOf(1)),
            "uri:3" to Result.success(byteArrayOf(2)),
        ))

        val result = scanner(documents, reader).scan("tree", 1)

        assertArrayEquals(byteArrayOf(1), result.albums.single().embeddedArtwork)
        assertEquals(listOf("uri:1", "uri:2"), reader.artworkUris)
    }

    @Test fun artworkFailureContinuesAndCancellationReturnsCancelled() {
        var cancel = false
        val reader = FakeMetadataReader(
            artwork = mutableMapOf(
                "uri:1" to Result.failure(IllegalStateException("bad art")),
                "uri:2" to Result.success(byteArrayOf(1)),
            ),
            afterArtwork = { if (it == "uri:1") cancel = true },
        )
        val result = scanner(
            listOf(document("Album/01.mp3", "uri:1"), document("Album/02.mp3", "uri:2")),
            reader,
        ).scan("tree", 1, cancelled = { cancel })

        assertEquals(ScanOutcome.CANCELLED, result.outcome)
        assertEquals(listOf("uri:1"), reader.artworkUris)
    }

    private fun document(path: String, uri: String) = DiscoveredDocument(path, uri, path.substringAfterLast('/'), 42, 1_000)

    private fun scanner(documents: List<DiscoveredDocument>, reader: FakeMetadataReader) =
        LibraryScanner(FakeEnumerator(documents), reader)

    private class FakeEnumerator(private val documents: List<DiscoveredDocument>) : LibraryDocumentEnumerator {
        override fun canAccess(treeUri: String) = true
        override fun enumerate(treeUri: String, cancelled: () -> Boolean) = documents.takeUnless { cancelled() }.orEmpty()
    }

    private class FakeMetadataReader(
        private val tags: MutableMap<String, Result<ReadMetadata>> = mutableMapOf(),
        private val artwork: MutableMap<String, Result<ByteArray?>> = mutableMapOf(),
        private val afterArtwork: (String) -> Unit = {},
    ) : MediaMetadataReader {
        val tagUris = mutableListOf<String>()
        val artworkUris = mutableListOf<String>()

        override fun readTags(documentUri: String): Result<ReadMetadata> {
            tagUris += documentUri
            return tags[documentUri] ?: Result.success(ReadMetadata(null, null, null, null, null, null, 1_000))
        }

        override fun readArtwork(documentUri: String): Result<ByteArray?> {
            artworkUris += documentUri
            return (artwork[documentUri] ?: Result.success(null)).also { afterArtwork(documentUri) }
        }

        fun clearCalls() {
            tagUris.clear()
            artworkUris.clear()
        }
    }
}
