package ca.stewark.nocturnel.artwork

class PixelBuffer(val width: Int, val height: Int, pixels: IntArray) {
    private val values = pixels.copyOf()

    init {
        require(width > 0 && height > 0) { "Pixel dimensions must be positive." }
        require(values.size == width * height) { "Pixel count must equal width × height." }
    }

    fun pixels(): IntArray = values.copyOf()
    operator fun get(index: Int): Int = values[index]

    override fun equals(other: Any?): Boolean =
        other is PixelBuffer && width == other.width && height == other.height && values.contentEquals(other.values)

    override fun hashCode(): Int = 31 * (31 * width + height) + values.contentHashCode()
}
