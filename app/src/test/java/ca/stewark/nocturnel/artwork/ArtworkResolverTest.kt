package ca.stewark.nocturnel.artwork

import ca.stewark.nocturnel.library.model.ArtworkKind
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkResolverTest {
    @Test
    fun artworkPrecedenceIsManualThenEmbeddedThenFolderCoverThenPlaceholder() {
        assertEquals(ArtworkKind.MANUAL, ArtworkResolver.resolve(ArtworkCandidates("manual", "embedded", "folder")).kind)
        assertEquals(ArtworkKind.EMBEDDED, ArtworkResolver.resolve(ArtworkCandidates(embeddedUri = "embedded", folderCoverUri = "folder")).kind)
        assertEquals(ArtworkKind.FOLDER, ArtworkResolver.resolve(ArtworkCandidates(folderCoverUri = "folder")).kind)
        assertEquals(ArtworkKind.PLACEHOLDER, ArtworkResolver.resolve(ArtworkCandidates()).kind)
    }
}
