package ca.stewark.nocturnel.playlist

data class M3u8ImportResult(val paths: List<String>, val skipped: List<String>)

object M3u8Codec {
    fun parse(text: String, knownPaths: Set<String>): M3u8ImportResult {
        val paths = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        playlistLines(text).forEach { raw ->
            val normalized = normalizedSafePath(raw)
            if (normalized != null && normalized in knownPaths) paths += normalized else skipped += raw
        }
        return M3u8ImportResult(paths, skipped)
    }

    fun parsePortable(text: String): M3u8ImportResult {
        val paths = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        playlistLines(text).forEach { raw ->
            val normalized = normalizedSafePath(raw)
            if (normalized == null) skipped += raw else paths += normalized
        }
        return M3u8ImportResult(paths, skipped)
    }

    private fun playlistLines(text: String) = text.lineSequence()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith('#') }

    private fun normalizedSafePath(raw: String): String? {
        val normalized = raw.replace('\\', '/').removePrefix("./")
        return normalized.takeIf {
            it.isNotBlank() && !it.startsWith('/') && !it.contains(":") &&
                it.split('/').none { segment -> segment == ".." }
        }
    }

    fun encode(paths: List<String>): String = buildString {
        append("#EXTM3U\n")
        paths.forEach { append(it).append('\n') }
    }
}
