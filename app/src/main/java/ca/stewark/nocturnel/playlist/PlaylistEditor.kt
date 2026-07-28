package ca.stewark.nocturnel.playlist

object PlaylistEditor {
    fun add(paths: List<String>, relativePath: String, index: Int = paths.size): List<String> {
        val target = index.coerceIn(0, paths.size)
        return paths.toMutableList().apply { add(target, relativePath) }
    }

    fun removeAt(paths: List<String>, index: Int): List<String> {
        require(index in paths.indices) { "Playlist entry index is out of range." }
        return paths.toMutableList().apply { removeAt(index) }
    }

    fun move(paths: List<String>, fromIndex: Int, toIndex: Int): List<String> {
        require(fromIndex in paths.indices) { "Source playlist entry index is out of range." }
        require(toIndex in paths.indices) { "Destination playlist entry index is out of range." }
        if (fromIndex == toIndex) return paths
        return paths.toMutableList().apply {
            val entry = removeAt(fromIndex)
            add(toIndex, entry)
        }
    }
}
