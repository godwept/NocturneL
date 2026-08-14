package ca.stewark.nocturnel.artwork

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation

class RetroArtworkTransformation(
    private val identity: String,
    private val paletteSize: Int = 16,
) : Transformation {
    override val cacheKey: String = RetroArtworkCacheKey(identity, paletteSize = paletteSize).toString()

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // The palette pass examines every pixel; full-resolution embedded covers stall the first grid render.
        val (width, height) = ArtworkSizing.transformedDimensions(input.width, input.height)
        val scaled = if (width == input.width && height == input.height) input else Bitmap.createScaledBitmap(input, width, height, true)
        val pixels = IntArray(scaled.width * scaled.height)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
        val result = AdaptivePaletteQuantizer(paletteSize).quantize(PixelBuffer(scaled.width, scaled.height, pixels))
        return Bitmap.createBitmap(result.pixels(), result.width, result.height, Bitmap.Config.ARGB_8888)
    }
}
