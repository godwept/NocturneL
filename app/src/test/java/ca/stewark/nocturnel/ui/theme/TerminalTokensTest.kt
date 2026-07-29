package ca.stewark.nocturnel.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalTokensTest {
    @Test fun `palette matches the PWA`() {
        assertEquals(0xFF000000.toInt(), TerminalBlack.toArgb())
        assertEquals(0xFF050805.toInt(), TerminalBlackAlt.toArgb())
        assertEquals(0xFF00FF41.toInt(), Phosphor.toArgb())
        assertEquals(0xFF00B32D.toInt(), PhosphorDim.toArgb())
        assertEquals(0xFF008020.toInt(), PhosphorMuted.toArgb())
        assertEquals(0xFF39FF7C.toInt(), PhosphorBright.toArgb())
        assertEquals(0xFFFFB000.toInt(), AlertAmber.toArgb())
        assertEquals(0xFFFF3030.toInt(), TerminalError.toArgb())
        assertEquals(.18f, ScanlineAlpha)
    }
}
