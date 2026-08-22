package ca.stewark.nocturnel.ui.listening

enum class LibraryViewMode(val label: String) {
    GRID("GRID"),
    FLOW("FLOW");

    fun next(): LibraryViewMode = entries[(ordinal + 1) % entries.size]

    companion object {
        val DEFAULT = GRID

        fun fromPersisted(value: String?): LibraryViewMode =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
