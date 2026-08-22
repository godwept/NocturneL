package ca.stewark.nocturnel.ui.theme

enum class ColorThemePreset(
    val persistedValue: String,
    val label: String,
) {
    GREEN_TERMINAL("green_terminal", "GREEN TERMINAL"),
    AMBER_TERMINAL("amber_terminal", "AMBER TERMINAL"),
    BLUE_TERMINAL("blue_terminal", "BLUE TERMINAL"),
    SYNTHWAVE_80S("80s_synthwave", "'80S SYNTHWAVE"),
    NEON_90S("90s_neon", "'90S NEON");

    fun next(): ColorThemePreset = entries[(ordinal + 1) % entries.size]

    companion object {
        val DEFAULT = GREEN_TERMINAL

        fun fromPersisted(value: String?): ColorThemePreset =
            entries.firstOrNull { it.persistedValue == value } ?: DEFAULT
    }
}
