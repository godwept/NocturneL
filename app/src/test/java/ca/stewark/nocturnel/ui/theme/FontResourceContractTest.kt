package ca.stewark.nocturnel.ui.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class FontResourceContractTest {
    @Test fun approvedFontFilesAndLicensesAreBundled() {
        val resources = File("src/main/res")
        val fonts = listOf(
            "font/vt323_regular.ttf",
            "font/share_tech_mono_regular.ttf",
            "font/oxanium_variable.ttf",
            "font/ibm_plex_mono_regular.ttf",
            "font/press_start_2p_regular.ttf",
            "font/space_mono_regular.ttf",
            "font/space_mono_bold.ttf",
        )
        val licenses = listOf(
            "raw/ofl_vt323.txt",
            "raw/ofl_share_tech_mono.txt",
            "raw/ofl_oxanium.txt",
            "raw/ofl_ibm_plex_mono.txt",
            "raw/ofl_press_start_2p.txt",
            "raw/ofl_space_mono.txt",
        )

        fonts.forEach { path ->
            val file = File(resources, path)
            assertTrue("Missing font resource: $path", file.isFile)
            assertTrue("Font resource is unexpectedly small: $path", file.length() > 1_000)
        }
        licenses.forEach { path ->
            val file = File(resources, path)
            assertTrue("Missing font license: $path", file.isFile)
            assertTrue(
                "Font license is not OFL 1.1: $path",
                file.readText().contains("SIL OPEN FONT LICENSE Version 1.1"),
            )
        }
    }
}
