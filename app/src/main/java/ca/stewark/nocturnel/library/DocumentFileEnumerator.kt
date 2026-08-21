package ca.stewark.nocturnel.library

import androidx.documentfile.provider.DocumentFile

class DocumentFileEnumerator private constructor(
    private val access: (String) -> Boolean,
    private val openTree: (String) -> DocumentFile?,
) : LibraryDocumentEnumerator {
    constructor(treeAccess: LibraryTreeAccess) : this(treeAccess::canRead, treeAccess::openTree)

    internal constructor(openTree: (String) -> DocumentFile?) : this({ true }, openTree)

    override fun canAccess(treeUri: String): Boolean = access(treeUri)

    override fun enumerate(treeUri: String, cancelled: () -> Boolean): List<DiscoveredDocument> {
        val root = openTree(treeUri)?.takeIf { it.canRead() }
            ?: throw LibraryEnumerationAccessException()
        return try {
            DocumentTreeWalker.walk(root, cancelled).toList()
        } catch (error: Exception) {
            throw LibraryEnumerationAccessException(error)
        }
    }
}
