package ca.stewark.nocturnel.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class FontPresetTest {
    @Test fun stableValuesLabelsAndCycleOrderAreFixed() {
        assertEquals(
            listOf("classic", "mainframe", "pixel", "modern"),
            FontPreset.entries.map { it.persistedValue },
        )
        assertEquals(
            listOf("CLASSIC", "MAINFRAME", "PIXEL", "MODERN"),
            FontPreset.entries.map { it.label },
        )
        assertEquals(FontPreset.MAINFRAME, FontPreset.CLASSIC.next())
        assertEquals(FontPreset.PIXEL, FontPreset.MAINFRAME.next())
        assertEquals(FontPreset.MODERN, FontPreset.PIXEL.next())
        assertEquals(FontPreset.CLASSIC, FontPreset.MODERN.next())
    }

    @Test fun persistedValuesRestoreOrFallBackToClassic() {
        assertEquals(FontPreset.CLASSIC, FontPreset.fromPersisted(null))
        assertEquals(FontPreset.CLASSIC, FontPreset.fromPersisted("unknown"))
        assertEquals(FontPreset.MODERN, FontPreset.fromPersisted("modern"))
    }
}
