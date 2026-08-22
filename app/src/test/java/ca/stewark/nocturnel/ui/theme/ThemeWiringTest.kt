package ca.stewark.nocturnel.ui.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeWiringTest {
    @Test fun themeResolvesTypographyFromTheSelectedPreset() {
        val source = File("src/main/java/ca/stewark/nocturnel/ui/theme/Theme.kt").readText()

        assertTrue("fontPreset: FontPreset = FontPreset.DEFAULT" in source)
        assertTrue("typography = typographyFor(fontPreset)" in source)
    }
}
