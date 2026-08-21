package ca.stewark.nocturnel.ui.listening

enum class LibrarySortMode(val label: String) {
    ARTIST("ARTIST"),
    TITLE("TITLE"),
    YEAR("YEAR"),
    MOST_PLAYED("MOST PLAYED");

    fun next(): LibrarySortMode = entries[(ordinal + 1) % entries.size]

    companion object {
        val DEFAULT = ARTIST

        fun fromPersisted(value: String?): LibrarySortMode =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
