package ca.stewark.nocturnel.artwork

import ca.stewark.nocturnel.library.model.ArtworkKind
import ca.stewark.nocturnel.library.model.ArtworkSource

data class ArtworkCandidates(
    val manualUri: String? = null,
    val embeddedUri: String? = null,
    val folderCoverUri: String? = null,
)

object ArtworkResolver {
    fun resolve(candidates: ArtworkCandidates): ArtworkSource = when {
        !candidates.manualUri.isNullOrBlank() -> ArtworkSource(ArtworkKind.MANUAL, candidates.manualUri)
        !candidates.embeddedUri.isNullOrBlank() -> ArtworkSource(ArtworkKind.EMBEDDED, candidates.embeddedUri)
        !candidates.folderCoverUri.isNullOrBlank() -> ArtworkSource(ArtworkKind.FOLDER, candidates.folderCoverUri)
        else -> ArtworkSource(ArtworkKind.PLACEHOLDER)
    }

    fun isFolderCoverFile(name: String): Boolean = name.lowercase() in setOf("cover.jpg", "folder.jpg", "albumart.jpg", "front.jpg")
}
