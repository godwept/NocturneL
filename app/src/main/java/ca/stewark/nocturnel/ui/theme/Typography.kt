package ca.stewark.nocturnel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import ca.stewark.nocturnel.R

private val TerminalDisplay = FontFamily(Font(R.font.vt323_regular))
private val TerminalMono = FontFamily(Font(R.font.share_tech_mono_regular))

val NocturneLTypography = Typography(
    displayLarge = TextStyle(fontFamily = TerminalDisplay, fontSize = 48.sp, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontFamily = TerminalDisplay, fontSize = 36.sp, lineHeight = 38.sp),
    titleLarge = TextStyle(fontFamily = TerminalDisplay, fontSize = 28.sp, lineHeight = 30.sp, letterSpacing = 1.5.sp),
    titleMedium = TextStyle(fontFamily = TerminalDisplay, fontSize = 22.sp, lineHeight = 24.sp, letterSpacing = 1.sp),
    bodyLarge = TextStyle(fontFamily = TerminalMono, fontSize = 17.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = TerminalMono, fontSize = 15.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = TerminalMono, fontSize = 14.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = TerminalMono, fontSize = 12.sp, lineHeight = 16.sp),
)
