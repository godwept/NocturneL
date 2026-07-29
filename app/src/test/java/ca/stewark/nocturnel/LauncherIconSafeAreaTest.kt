package ca.stewark.nocturnel

import java.io.File
import javax.imageio.ImageIO
import kotlin.math.hypot
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconSafeAreaTest {
    @Test fun adaptiveForegroundFitsInsideSafeCircle() {
        val foreground = sequenceOf(
            File("src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png"),
            File("app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png"),
        ).first(File::isFile)
        val image = ImageIO.read(foreground)
        val centerX = image.width / 2.0
        val centerY = image.height / 2.0
        val safeRadius = image.width * 66.0 / 108.0 / 2.0
        var maximumRadius = 0.0

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val alpha = image.getRGB(x, y).ushr(24)
                if (alpha > 32) {
                    maximumRadius = maxOf(
                        maximumRadius,
                        hypot(x + 0.5 - centerX, y + 0.5 - centerY),
                    )
                }
            }
        }

        assertTrue(
            "Foreground radius $maximumRadius exceeds adaptive safe radius $safeRadius",
            maximumRadius <= safeRadius,
        )
    }
}
