package ca.stewark.nocturnel.library

data class DiscoveredDocument(
    val relativePath: String,
    val documentUri: String,
    val displayName: String,
    val fileSizeBytes: Long?,
    val lastModifiedEpochMillis: Long?,
) {
    val fingerprint: FileFingerprint
        get() = FileFingerprint(fileSizeBytes, lastModifiedEpochMillis)
}

interface LibraryDocumentEnumerator {
    fun canAccess(treeUri: String): Boolean
    fun enumerate(treeUri: String, cancelled: () -> Boolean = { false }): List<DiscoveredDocument>
}

class LibraryEnumerationAccessException(cause: Throwable? = null) : IllegalStateException(cause)
