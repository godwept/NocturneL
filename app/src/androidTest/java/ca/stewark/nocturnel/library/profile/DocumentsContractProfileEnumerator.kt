package ca.stewark.nocturnel.library.profile

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import ca.stewark.nocturnel.library.DiscoveredDocument
import ca.stewark.nocturnel.library.LibraryDocumentEnumerator
import ca.stewark.nocturnel.library.LibraryEnumerationAccessException
import ca.stewark.nocturnel.library.SupportedAudioFormats

class DocumentsContractProfileEnumerator(
    private val resolver: ContentResolver,
) : LibraryDocumentEnumerator {
    override fun canAccess(treeUri: String): Boolean = runCatching {
        val uri = Uri.parse(treeUri)
        val id = DocumentsContract.getTreeDocumentId(uri)
        resolver.query(
            DocumentsContract.buildDocumentUriUsingTree(uri, id),
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null,
            null,
            null,
        )?.use { it.moveToFirst() } == true
    }.getOrDefault(false)

    override fun enumerate(treeUri: String, cancelled: () -> Boolean): List<DiscoveredDocument> = try {
        val tree = Uri.parse(treeUri)
        val output = mutableListOf<DiscoveredDocument>()

        fun visit(parentId: String, prefix: String) {
            if (cancelled()) return
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
            resolver.query(children, PROJECTION, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (!cancelled() && cursor.moveToNext()) {
                    val id = cursor.getString(idColumn)
                    val name = cursor.getString(nameColumn) ?: continue
                    val path = if (prefix.isBlank()) name else "$prefix/$name"
                    if (cursor.getString(mimeColumn) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        visit(id, path)
                    } else {
                        val audio = SupportedAudioFormats.isCandidateAudioFile(name)
                        val size = if (audio && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn).takeIf { it >= 0 } else null
                        val modified = if (audio && !cursor.isNull(modifiedColumn)) cursor.getLong(modifiedColumn).takeIf { it > 0 } else null
                        output += DiscoveredDocument(
                            relativePath = path,
                            documentUri = DocumentsContract.buildDocumentUriUsingTree(tree, id).toString(),
                            displayName = name,
                            fileSizeBytes = size,
                            lastModifiedEpochMillis = modified,
                        )
                    }
                }
            } ?: throw LibraryEnumerationAccessException()
        }

        visit(DocumentsContract.getTreeDocumentId(tree), "")
        output
    } catch (error: LibraryEnumerationAccessException) {
        throw error
    } catch (error: RuntimeException) {
        throw LibraryEnumerationAccessException(error)
    }

    private companion object {
        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }
}
