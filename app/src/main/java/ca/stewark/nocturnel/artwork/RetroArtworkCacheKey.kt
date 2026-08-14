package ca.stewark.nocturnel.artwork

data class RetroArtworkCacheKey(
    val sourceIdentity: String,
    val modificationToken: String = "",
    val paletteSize: Int = 16,
    val ditherVersion: Int = DITHER_VERSION,
) {
    override fun toString(): String =
        "retro-artwork-v$ALGORITHM_VERSION:$sourceIdentity:$modificationToken:$paletteSize:$ditherVersion"

    companion object {
        const val ALGORITHM_VERSION = 2
        const val DITHER_VERSION = 1
    }
}
