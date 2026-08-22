package ca.stewark.nocturnel.ui.theme

enum class FontPreset(
    val persistedValue: String,
    val label: String,
) {
    CLASSIC("classic", "CLASSIC"),
    MAINFRAME("mainframe", "MAINFRAME"),
    PIXEL("pixel", "PIXEL"),
    MODERN("modern", "MODERN");

    fun next(): FontPreset = entries[(ordinal + 1) % entries.size]

    companion object {
        val DEFAULT = CLASSIC

        fun fromPersisted(value: String?): FontPreset =
            entries.firstOrNull { it.persistedValue == value } ?: DEFAULT
    }
}
