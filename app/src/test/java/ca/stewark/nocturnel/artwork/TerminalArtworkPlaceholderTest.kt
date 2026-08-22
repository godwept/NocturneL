package ca.stewark.nocturnel.artwork

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalArtworkPlaceholderTest {
    @Test fun accentSelectionIsDeterministicWithinTheActivePalette() {
        val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow)
        val first = TerminalArtworkPlaceholder.accentFor("album", colors)

        assertEquals(first, TerminalArtworkPlaceholder.accentFor("album", colors))
        assertEquals(true, first in colors)
        assertEquals(Color.Magenta, TerminalArtworkPlaceholder.accentFor("album", listOf(Color.Magenta)))
    }
}
