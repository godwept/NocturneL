package ca.stewark.nocturnel.artwork

import kotlin.math.roundToInt

internal object ArtworkSizing {
    const val MAX_TRANSFORM_DIMENSION = 512

    fun transformedDimensions(width: Int, height: Int): Pair<Int, Int> {
        require(width > 0 && height > 0)
        val largestDimension = maxOf(width, height)
        if (largestDimension <= MAX_TRANSFORM_DIMENSION) return width to height

        val scale = MAX_TRANSFORM_DIMENSION.toFloat() / largestDimension
        return (width * scale).roundToInt().coerceAtLeast(1) to
            (height * scale).roundToInt().coerceAtLeast(1)
    }
}
