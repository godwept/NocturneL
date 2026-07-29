package ca.stewark.nocturnel.artwork

import kotlin.math.max
import kotlin.math.min

class AdaptivePaletteQuantizer(
    private val maximumColors: Int = 16,
    private val dither: Boolean = true,
) {
    init {
        require(maximumColors in 1..256)
    }

    fun palette(input: PixelBuffer): List<Int> {
        val histogram = input.pixels()
            .asSequence()
            .filter { alpha(it) != 0 }
            .groupingBy { it and 0x00FFFFFF }
            .eachCount()
        if (histogram.isEmpty()) return emptyList()
        if (histogram.size <= maximumColors) return histogram.keys.sorted()

        data class WeightedColor(val rgb: Int, val count: Int)
        data class Bucket(val colors: List<WeightedColor>) {
            fun range(channel: (Int) -> Int): Int =
                colors.maxOf { channel(it.rgb) } - colors.minOf { channel(it.rgb) }
            val population: Int get() = colors.sumOf { it.count }
        }

        val buckets = mutableListOf(Bucket(histogram.entries.map { WeightedColor(it.key, it.value) }))
        while (buckets.size < maximumColors) {
            val splitIndex = buckets.indices
                .filter { buckets[it].colors.size > 1 }
                .maxByOrNull { index ->
                    val bucket = buckets[index]
                    maxOf(bucket.range(::red), bucket.range(::green), bucket.range(::blue)) * bucket.population
                } ?: break
            val bucket = buckets.removeAt(splitIndex)
            val channel: (Int) -> Int = when (maxOf(bucket.range(::red), bucket.range(::green), bucket.range(::blue))) {
                bucket.range(::red) -> ::red
                bucket.range(::green) -> ::green
                else -> ::blue
            }
            val sorted = bucket.colors.sortedWith(compareBy<WeightedColor> { channel(it.rgb) }.thenBy { it.rgb })
            val half = bucket.population / 2
            var total = 0
            var cut = 1
            for (index in 0 until sorted.lastIndex) {
                total += sorted[index].count
                if (total >= half) {
                    cut = index + 1
                    break
                }
            }
            buckets += Bucket(sorted.take(cut))
            buckets += Bucket(sorted.drop(cut))
        }
        return buckets.map { bucket ->
            val count = bucket.population.coerceAtLeast(1)
            rgb(
                bucket.colors.sumOf { red(it.rgb) * it.count } / count,
                bucket.colors.sumOf { green(it.rgb) * it.count } / count,
                bucket.colors.sumOf { blue(it.rgb) * it.count } / count,
            )
        }.sorted()
    }

    fun quantize(input: PixelBuffer): PixelBuffer {
        val palette = palette(input)
        if (palette.isEmpty()) return input
        val source = input.pixels()
        val output = IntArray(source.size)
        source.forEachIndexed { index, color ->
            val a = alpha(color)
            if (a == 0) {
                output[index] = color
            } else {
                val x = index % input.width
                val y = index / input.width
                val offset = if (dither) (BAYER_4[y and 3][x and 3] - 7.5f) * 1.8f else 0f
                val adjusted = rgb(
                    (red(color) + offset).toInt().coerceIn(0, 255),
                    (green(color) + offset).toInt().coerceIn(0, 255),
                    (blue(color) + offset).toInt().coerceIn(0, 255),
                )
                output[index] = (a shl 24) or nearest(adjusted, palette)
            }
        }
        return PixelBuffer(input.width, input.height, output)
    }

    private fun nearest(color: Int, palette: List<Int>): Int = palette.minBy { candidate ->
        val meanRed = (red(color) + red(candidate)) / 2
        val dr = red(color) - red(candidate)
        val dg = green(color) - green(candidate)
        val db = blue(color) - blue(candidate)
        ((512 + meanRed) * dr * dr shr 8) + 4 * dg * dg + ((767 - meanRed) * db * db shr 8)
    }

    private companion object {
        val BAYER_4 = arrayOf(
            intArrayOf(0, 8, 2, 10),
            intArrayOf(12, 4, 14, 6),
            intArrayOf(3, 11, 1, 9),
            intArrayOf(15, 7, 13, 5),
        )
        fun alpha(color: Int) = color ushr 24
        fun red(color: Int) = color shr 16 and 0xFF
        fun green(color: Int) = color shr 8 and 0xFF
        fun blue(color: Int) = color and 0xFF
        fun rgb(red: Int, green: Int, blue: Int) =
            (min(255, max(0, red)) shl 16) or (min(255, max(0, green)) shl 8) or min(255, max(0, blue))
    }
}
