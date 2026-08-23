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

    @Test fun `main navigation fills the width with heading-aligned equal touch targets`() {
        val source = File("src/main/java/ca/stewark/nocturnel/ui/components/TerminalNavigation.kt").readText()

        assertTrue("Navigation must align with the heading margin", ".padding(horizontal = TerminalDimensions.md)" in source)
        assertTrue("Navigation tabs must spread across the available width", "Arrangement.SpaceBetween" in source)
        assertTrue("Navigation tabs must retain minimum touch width", ".widthIn(min = TerminalDimensions.minimumTouchTarget)" in source)
        assertTrue("Navigation labels must begin at the heading edge", "contentAlignment = Alignment.CenterStart" in source)
        assertTrue("Navigation must use its compact label style", "MaterialTheme.typography.labelSmall" in source)
        assertTrue("Navigation should not collapse into a left-anchored scroll row", ".horizontalScroll(" !in source)
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
