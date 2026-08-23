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
    @Test fun phoneLibrary() = assertRequiredScreenshot("phone/01-library.png")
    @Test fun phoneAlbum() = assertRequiredScreenshot("phone/02-album.png")
    @Test fun phoneSpectrumBands() = assertRequiredScreenshot("phone/03-vis1.png")
    @Test fun phoneRadarVisualizer() = assertRequiredScreenshot("phone/04-vis2.png")
    @Test fun phoneNowPlayingAlbum() = assertRequiredScreenshot("phone/05-now-playing-album.png")

    @Test fun assetManifestDocumentsEveryImage() {
        val manifest = File(graphics, "README.md").readText()
        listOf(
            "icon.png", "feature-graphic.png", "phone/01-library.png", "phone/02-album.png",
            "phone/03-vis1.png", "phone/04-vis2.png", "phone/05-now-playing-album.png",
        ).forEach { assertTrue("Missing asset documentation: $it", it in manifest) }
    }

    @Test fun phoneManifestUsesTheApprovedOrderAndAccessibleAltText() {
        val manifest = File(graphics, "README.md").readText()
        val phoneRows = manifest.lineSequence().filter { it.startsWith("| phone/") }.toList()
        assertEquals(
            listOf(
                "phone/01-library.png",
                "phone/02-album.png",
                "phone/03-vis1.png",
                "phone/04-vis2.png",
                "phone/05-now-playing-album.png",
            ),
            phoneRows.map { row -> row.split('|')[1].trim() },
        )
        phoneRows.forEach { row ->
            val cells = row.trim('|').split('|').map(String::trim)
            val altText = cells[3]
            assertTrue("Missing alt text: $row", altText.isNotBlank())
            assertTrue("Alt text exceeds 140 characters: $altText", altText.length <= 140)
        }
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

    private fun assertRequiredScreenshot(relativePath: String) {
        val file = File(graphics, relativePath)
        assertTrue("Missing asset: ${file.path}", file.isFile)
        val image = ImageIO.read(file)
        assertEquals("Width for $relativePath", 1080, image.width)
        assertEquals("Height for $relativePath", 1920, image.height)
        val allPixelsOpaque = (0 until image.height).all { y ->
            image.getRGB(0, y, image.width, 1, null, 0, image.width)
                .all { pixel -> pixel ushr 24 == 0xff }
        }
        assertTrue("Transparent pixel in $relativePath", allPixelsOpaque)
        assertFalse("Unexpected alpha channel: $relativePath", image.colorModel.hasAlpha())
        assertEquals("Bit depth for $relativePath", 24, image.colorModel.pixelSize)
    }
}
