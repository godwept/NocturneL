package ca.stewark.nocturnel

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseVersionScriptTest {
    private val script = File("../.github/scripts/validate-release-version.sh")

    @Test fun codeOnePassesWithoutPriorReleaseTags() {
        assertEquals(0, runScript("0.1.0", "1", "play/0.1.0-1"))
    }

    @Test fun codeTwoPassesAfterCodeOne() {
        assertEquals(0, runScript("0.1.1", "2", "play/0.1.1-2", "play/0.1.0-1"))
    }

    @Test fun codeOneFailsAfterCodeOne() {
        assertEquals(1, runScript("0.1.1", "1", "play/0.1.1-1", "play/0.1.0-1"))
    }

    @Test fun codeOneFailsAfterCodeTwo() {
        assertEquals(1, runScript("0.1.1", "1", "play/0.1.1-1", "play/0.1.0-2"))
    }

    @Test fun mismatchedTagFails() {
        assertEquals(1, runScript("0.1.0", "1", "play/0.1.0-2"))
    }

    private fun runScript(vararg arguments: String): Int =
        ProcessBuilder(listOf("bash", script.canonicalPath) + arguments)
            .redirectErrorStream(true)
            .start()
            .also { it.inputStream.bufferedReader().use { output -> output.readText() } }
            .waitFor()
}
