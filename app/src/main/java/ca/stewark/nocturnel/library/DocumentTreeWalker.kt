package ca.stewark.nocturnel.library

import androidx.documentfile.provider.DocumentFile

data class DiscoveredDocument(val relativePath: String, val document: DocumentFile)

object DocumentTreeWalker {
    fun walk(root: DocumentFile, cancelled: () -> Boolean = { false }): Sequence<DiscoveredDocument> = sequence {
        suspend fun SequenceScope<DiscoveredDocument>.visit(folder: DocumentFile, prefix: String) {
            if (cancelled()) return
            folder.listFiles().forEach { child ->
                if (cancelled()) return
                val name = child.name ?: return@forEach
                val path = if (prefix.isBlank()) name else "$prefix/$name"
                when {
                    child.isDirectory -> visit(child, path)
                    child.isFile -> yield(DiscoveredDocument(path, child))
                }
            }
        }
        visit(root, "")
    }
}
