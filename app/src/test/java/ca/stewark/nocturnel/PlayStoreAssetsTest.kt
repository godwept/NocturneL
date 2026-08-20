package ca.stewark.nocturnel

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayStoreAssetsTest {
    private val graphics = File("../docs/play-store/listing/graphics")

    @Test fun storeIcon() = assertImage("icon.png", 512, 512, alpha = true, maxBytes = 1_024 * 1_024)
    @Test fun featureGraphic() = assertImage("feature-graphic.png", 1024, 500, alpha = false)
    @Test fun phoneLibrary() = assertImage("phone/01-library.png", 1080, 1920, alpha = false)
    @Test fun phoneAlbum() = assertImage("phone/02-album.png", 1080, 1920, alpha = false)
    @Test fun phoneNowPlaying() = assertImage("phone/03-now-playing.png", 1080, 1920, alpha = false)
    @Test fun phoneQueue() = assertImage("phone/04-queue.png", 1080, 1920, alpha = false)
    @Test fun tabletLibrary() = assertImage("tablet/01-library.png", 1920, 1080, alpha = false)
    @Test fun tabletAlbum() = assertImage("tablet/02-album.png", 1920, 1080, alpha = false)
    @Test fun tabletNowPlaying() = assertImage("tablet/03-now-playing.png", 1920, 1080, alpha = false)
    @Test fun tabletQueue() = assertImage("tablet/04-queue.png", 1920, 1080, alpha = false)

    @Test fun assetManifestDocumentsEveryImage() {
        val manifest = File(graphics, "README.md").readText()
        listOf(
            "icon.png", "feature-graphic.png", "phone/01-library.png", "phone/02-album.png",
            "phone/03-now-playing.png", "phone/04-queue.png", "tablet/01-library.png",
            "tablet/02-album.png", "tablet/03-now-playing.png", "tablet/04-queue.png",
        ).forEach { assertTrue("Missing asset documentation: $it", it in manifest) }
    }

    private fun assertImage(
        relativePath: String,
        width: Int,
        height: Int,
        alpha: Boolean,
        maxBytes: Long? = null,
    ) {
        val file = File(graphics, relativePath)
        assertTrue("Missing asset: ${file.path}", file.isFile)
        val image: BufferedImage = ImageIO.read(file)
        assertEquals("Width for $relativePath", width, image.width)
        assertEquals("Height for $relativePath", height, image.height)
        if (alpha) assertTrue("Expected alpha: $relativePath", image.colorModel.hasAlpha())
        else assertFalse("Unexpected alpha: $relativePath", image.colorModel.hasAlpha())
        maxBytes?.let { assertTrue("Asset too large: $relativePath", file.length() <= it) }
    }
}
