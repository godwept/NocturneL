package ca.stewark.nocturnel.playlist

data class M3u8ImportResult(val paths: List<String>, val skipped: List<String>)

object M3u8Codec {
    fun parse(text: String, knownPaths: Set<String>): M3u8ImportResult {
        val paths = mutableListOf<String>()
        val skipped = mutableListOf<String>()
        text.lineSequence().map(String::trim).filter { it.isNotEmpty() && !it.startsWith('#') }.forEach { raw ->
            val normalized = raw.replace('\\', '/').removePrefix("./")
            if (normalized.startsWith('/') || normalized.contains(":") || normalized.split('/').any { it == ".." } || normalized !in knownPaths) skipped += raw else paths += normalized
        }
        return M3u8ImportResult(paths, skipped)
    }

    fun encode(paths: List<String>): String = buildString {
        append("#EXTM3U\n")
        paths.forEach { append(it).append('\n') }
    }
}
