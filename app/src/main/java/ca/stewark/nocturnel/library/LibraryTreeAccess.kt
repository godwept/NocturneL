package ca.stewark.nocturnel.library

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class LibraryTreeAccess(private val context: Context) {
    fun persist(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun canRead(uri: String): Boolean = runCatching {
        val document = DocumentFile.fromTreeUri(context, Uri.parse(uri))
        document?.canRead() == true
    }.getOrDefault(false)

    fun displayName(uri: Uri): String = DocumentFile.fromTreeUri(context, uri)?.name ?: "Music folder"

    fun openTree(uri: String): DocumentFile? = DocumentFile.fromTreeUri(context, Uri.parse(uri))

    companion object {
        const val readFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        fun createTreeIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(readFlags)
    }
}
