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
        val pixels = IntArray(input.width * input.height)
        input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
        val result = AdaptivePaletteQuantizer(paletteSize).quantize(PixelBuffer(input.width, input.height, pixels))
        return Bitmap.createBitmap(result.pixels(), result.width, result.height, Bitmap.Config.ARGB_8888)
    }
}
