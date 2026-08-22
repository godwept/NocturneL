package ca.stewark.nocturnel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ca.stewark.nocturnel.R
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyTest {
    @Test fun everyPresetMapsEveryMaterialRoleToItsApprovedPair() {
        val expected = mapOf(
            FontPreset.CLASSIC to Pair(
                FontFamily(Font(R.font.vt323_regular)),
                FontFamily(Font(R.font.share_tech_mono_regular)),
            ),
            FontPreset.MAINFRAME to Pair(
                FontFamily(Font(R.font.oxanium_variable)),
                FontFamily(Font(R.font.ibm_plex_mono_regular)),
            ),
            FontPreset.PIXEL to Pair(
                FontFamily(Font(R.font.press_start_2p_regular)),
                FontFamily(Font(R.font.space_mono_regular)),
            ),
            FontPreset.MODERN to Pair(
                FontFamily(Font(R.font.space_mono_bold, weight = FontWeight.Bold)),
                FontFamily(Font(R.font.ibm_plex_mono_regular)),
            ),
        )

        expected.forEach { (preset, pair) ->
            val typography = typographyFor(preset)
            displayStyles(typography).forEach { style ->
                assertEquals("$preset display family", pair.first, style.fontFamily)
            }
            bodyStyles(typography).forEach { style ->
                assertEquals("$preset body family", pair.second, style.fontFamily)
            }
        }
    }

    @Test fun approvedMetricsRemainStable() {
        val typography = typographyFor(FontPreset.CLASSIC)

        assertEquals(48.sp, typography.displayLarge.fontSize)
        assertEquals(48.sp, typography.displayLarge.lineHeight)
        assertEquals(36.sp, typography.headlineLarge.fontSize)
        assertEquals(38.sp, typography.headlineLarge.lineHeight)
        assertEquals(28.sp, typography.titleLarge.fontSize)
        assertEquals(30.sp, typography.titleLarge.lineHeight)
        assertEquals(1.5.sp, typography.titleLarge.letterSpacing)
        assertEquals(22.sp, typography.titleMedium.fontSize)
        assertEquals(24.sp, typography.titleMedium.lineHeight)
        assertEquals(1.sp, typography.titleMedium.letterSpacing)
        assertEquals(17.sp, typography.bodyLarge.fontSize)
        assertEquals(22.sp, typography.bodyLarge.lineHeight)
        assertEquals(15.sp, typography.bodyMedium.fontSize)
        assertEquals(20.sp, typography.bodyMedium.lineHeight)
        assertEquals(14.sp, typography.labelLarge.fontSize)
        assertEquals(18.sp, typography.labelLarge.lineHeight)
        assertEquals(12.sp, typography.labelMedium.fontSize)
        assertEquals(16.sp, typography.labelMedium.lineHeight)
    }

    @Test fun pixelTitleTrackingKeepsCompactFrameTitlesOnOneLine() {
        assertEquals((-4).sp, typographyFor(FontPreset.PIXEL).titleMedium.letterSpacing)
    }

    private fun displayStyles(typography: Typography): List<TextStyle> = listOf(
        typography.displayLarge,
        typography.displayMedium,
        typography.displaySmall,
        typography.headlineLarge,
        typography.headlineMedium,
        typography.headlineSmall,
        typography.titleLarge,
        typography.titleMedium,
        typography.titleSmall,
    )

    private fun bodyStyles(typography: Typography): List<TextStyle> = listOf(
        typography.bodyLarge,
        typography.bodyMedium,
        typography.bodySmall,
        typography.labelLarge,
        typography.labelMedium,
        typography.labelSmall,
    )
}
