package ca.stewark.nocturnel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ca.stewark.nocturnel.R

private val ClassicDisplay = FontFamily(Font(R.font.vt323_regular))
private val ClassicBody = FontFamily(Font(R.font.share_tech_mono_regular))
private val MainframeDisplay = FontFamily(Font(R.font.oxanium_variable))
private val MainframeBody = FontFamily(Font(R.font.ibm_plex_mono_regular))
private val PixelDisplay = FontFamily(Font(R.font.press_start_2p_regular))
private val PixelBody = FontFamily(Font(R.font.space_mono_regular))
private val ModernDisplay = FontFamily(Font(R.font.space_mono_bold, weight = FontWeight.Bold))
private val ModernBody = MainframeBody

internal fun typographyFor(preset: FontPreset): Typography {
    val (display, body) = when (preset) {
        FontPreset.CLASSIC -> ClassicDisplay to ClassicBody
        FontPreset.MAINFRAME -> MainframeDisplay to MainframeBody
        FontPreset.PIXEL -> PixelDisplay to PixelBody
        FontPreset.MODERN -> ModernDisplay to ModernBody
    }
    val defaults = Typography()
    val titleMediumLetterSpacing = if (preset == FontPreset.PIXEL) (-4).sp else 1.sp
    return Typography(
        displayLarge = TextStyle(fontFamily = display, fontSize = 48.sp, lineHeight = 48.sp),
        displayMedium = defaults.displayMedium.copy(fontFamily = display),
        displaySmall = defaults.displaySmall.copy(fontFamily = display),
        headlineLarge = TextStyle(fontFamily = display, fontSize = 36.sp, lineHeight = 38.sp),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = display),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = display),
        titleLarge = TextStyle(fontFamily = display, fontSize = 28.sp, lineHeight = 30.sp, letterSpacing = 1.5.sp),
        titleMedium = TextStyle(fontFamily = display, fontSize = 22.sp, lineHeight = 24.sp, letterSpacing = titleMediumLetterSpacing),
        titleSmall = defaults.titleSmall.copy(fontFamily = display),
        bodyLarge = TextStyle(fontFamily = body, fontSize = 17.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontFamily = body, fontSize = 15.sp, lineHeight = 20.sp),
        bodySmall = defaults.bodySmall.copy(fontFamily = body),
        labelLarge = TextStyle(fontFamily = body, fontSize = 14.sp, lineHeight = 18.sp),
        labelMedium = TextStyle(fontFamily = body, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = defaults.labelSmall.copy(fontFamily = body),
    )
}
