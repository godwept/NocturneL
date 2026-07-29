package ca.stewark.nocturnel.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalUiSourceGuardTest {
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
}
