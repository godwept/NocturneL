package ca.stewark.nocturnel.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class TerminalUiSourceGuardTest {
    @Test fun `font-sensitive action groups wrap instead of clipping`() {
        val component = File("src/main/java/ca/stewark/nocturnel/ui/components/TerminalActionRow.kt")
        assertTrue("A shared wrapping action row must exist", component.isFile)

        val album = File("src/main/java/ca/stewark/nocturnel/ui/library/AlbumDetailScreen.kt").readText()
        val playlist = File("src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt").readText()
        assertTrue("Action rows must wrap controls that exceed the available width", "FlowRow(" in component.readText())
        assertEquals(2, Regex("TerminalActionRow \\{").findAll(album).count())
        assertEquals(1, Regex("TerminalActionRow \\{").findAll(playlist).count())
    }

    @Test fun `main navigation fills the width with equal touch targets`() {
        val source = File("src/main/java/ca/stewark/nocturnel/ui/components/TerminalNavigation.kt").readText()

        assertTrue("Navigation must fill the available width", ".fillMaxWidth()" in source)
        assertFalse("Navigation must not inset constrained labels", ".padding(horizontal" in source)
        assertTrue("Settings must be excluded from primary navigation", "filterNot { it == NocturneLDestination.SETTINGS }" in source)
        assertTrue("Navigation tabs must receive equal width", ".weight(1f)" in source)
        assertTrue("Navigation labels must be centered", "contentAlignment = Alignment.Center" in source)
        assertTrue("Primary navigation must use the prominent label style", "textStyle = MaterialTheme.typography.labelLarge" in source)
        assertFalse("Primary navigation must not use the small label style", "MaterialTheme.typography.labelSmall" in source)
        assertTrue("Navigation brackets must omit decorative spaces", "spacedBrackets = false" in source)
        assertFalse("Navigation must not force a font size", ".fontSize" in source)
        assertFalse("Navigation must not force letter spacing", ".letterSpacing" in source)
        assertFalse("Navigation tabs must not use minimum widths", ".widthIn" in source)
        assertFalse("Equal tabs must not use space-between arrangement", "Arrangement.SpaceBetween" in source)
        assertTrue("Navigation should not collapse into a left-anchored scroll row", ".horizontalScroll(" !in source)
    }

    @Test fun `settings is an accessible icon in the title row`() {
        val source = File("src/main/java/ca/stewark/nocturnel/ui/components/TerminalScaffold.kt").readText()

        assertTrue("Header must be a full-width row", "Row(Modifier.fillMaxWidth()" in source)
        assertTrue("Header contents must be vertically centered", "verticalAlignment = Alignment.CenterVertically" in source)
        assertTrue("Settings must sit opposite the title", "horizontalArrangement = Arrangement.SpaceBetween" in source)
        assertTrue("Header must retain the title", "\"NOCTURNEL\"" in source)
        assertTrue("Header must use the local Settings vector", "R.drawable.ic_settings" in source)
        assertTrue("Settings must have an accessible label", "\"Settings\"" in source)
        assertTrue("Settings must use the terminal icon control", "TerminalIconButton(" in source)
        assertTrue("Settings selection must follow the destination", "selected == NocturneLDestination.SETTINGS" in source)
        assertTrue("Settings must reuse the navigation pulse", "rememberActiveNavigationPulse" in source)
    }

    @Test fun `production ui uses semantic theme colors`() {
        val root = File("src/main/java/ca/stewark/nocturnel/ui")
        val forbidden = listOf(
            "TerminalBlack", "TerminalBlackAlt", "Phosphor", "PhosphorDim", "PhosphorMuted",
            "PhosphorBright", "AlertAmber", "TerminalError", "TerminalText", "TerminalPanel",
        )
        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "Color.kt" }
            .flatMap { file ->
                val source = file.readText()
                forbidden.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(source) }
                    .map { "${file.name}: $it" }
            }
            .toList()
        assertTrue("Fixed terminal colors remain: $violations", violations.isEmpty())

        val uiSource = root.walkTopDown().filter { it.isFile && it.extension == "kt" && it.name != "Color.kt" }
            .joinToString("\n") { it.readText() }
        listOf("Color.Red", "Color.Blue", "Color.Green", "Color.Yellow", "Color.Black", "Color.White").forEach {
            assertFalse("Direct UI color remains: $it", it in uiSource)
        }
    }

    @Test fun `screens do not use material shaped controls directly`() {
        val root = File("src/main/java/ca/stewark/nocturnel/ui")
        val forbidden = listOf(
            Regex("""(?<![A-Za-z])Button\("""),
            Regex("""(?<![A-Za-z])OutlinedTextField\("""),
            Regex("""(?<![A-Za-z])Card\("""),
            Regex("""(?<![A-Za-z])RoundedCornerShape\("""),
        )
        val violations = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.parentFile?.name != "components" }
            .flatMap { file -> forbidden.filter { pattern -> pattern.containsMatchIn(file.readText()) }.map { "${file.name}: ${it.pattern}" } }
            .toList()
        assertTrue("Material-shaped controls remain: $violations", violations.isEmpty())
    }

    @Test fun `album artwork first paint is not blocked by a bitmap transformation`() {
        val artworkSource = File("src/main/java/ca/stewark/nocturnel/ui/artwork/RetroArtwork.kt").readText()

        assertTrue(
            "RetroArtwork must display the decoded source without waiting for a CPU bitmap transformation.",
            ".transformations(" !in artworkSource,
        )
        assertTrue("Album artwork must not be tinted by a color filter.", "colorFilter" !in artworkSource)
        assertTrue("Album artwork must not be recolored by a matrix.", "ColorMatrix" !in artworkSource)
    }
}
