package ca.stewark.nocturnel.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalSeekBarTest {
    @Test fun `seek fraction clamps to bounds`() {
        assertEquals(0f, seekFraction(-10f, 100f))
        assertEquals(.5f, seekFraction(50f, 100f))
        assertEquals(1f, seekFraction(110f, 100f))
        assertEquals(0f, seekFraction(1f, 0f))
    }
}
