package ca.stewark.nocturnel.library

import androidx.documentfile.provider.DocumentFile

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
                    child.isFile -> {
                        val audio = SupportedAudioFormats.isCandidateAudioFile(name)
                        yield(DiscoveredDocument(
                            relativePath = path,
                            documentUri = child.uri.toString(),
                            displayName = name,
                            fileSizeBytes = if (audio) child.length().takeIf { it >= 0 } else null,
                            lastModifiedEpochMillis = if (audio) child.lastModified().takeIf { it > 0 } else null,
                        ))
                    }
                }
            }
        }
        visit(root, "")
    }
}
