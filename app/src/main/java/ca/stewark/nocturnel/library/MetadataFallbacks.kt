package ca.stewark.nocturnel.library

data class FallbackMetadata(val title: String, val album: String, val artist: String, val trackNumber: Int?)

object MetadataFallbacks {
    private val numberedName = Regex("^\\s*(\\d{1,3})\\s*[-_.]\\s*(.+?)\\s*$")

    fun fromPath(relativePath: String): FallbackMetadata {
        val pieces = relativePath.trim('/').split('/').filter(String::isNotBlank)
        val filename = pieces.lastOrNull().orEmpty().substringBeforeLast('.', pieces.lastOrNull().orEmpty())
        val match = numberedName.matchEntire(filename)
        val title = match?.groupValues?.get(2)?.trim().takeUnless { it.isNullOrBlank() } ?: filename
        val number = match?.groupValues?.get(1)?.toIntOrNull()
        val album = pieces.dropLast(1).lastOrNull().orEmpty().ifBlank { "Unknown album" }
        val artist = pieces.dropLast(2).lastOrNull().orEmpty().ifBlank { "Unknown artist" }
        return FallbackMetadata(title.ifBlank { "Unknown track" }, album, artist, number)
    }

    fun preferred(value: String?, fallback: String): String = value?.trim().takeUnless { it.isNullOrBlank() } ?: fallback
}
