package ca.stewark.nocturnel.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class TerminalPalette(
    val background: Color,
    val panel: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val borderEmphasis: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val selection: Color,
    val warning: Color,
    val error: Color,
    val visualizerPrimary: Color,
    val visualizerSecondary: Color,
    val visualizerPeak: Color,
    val scanlineTint: Color,
    val artworkPlaceholderColors: List<Color>,
    val glowColor: Color,
    val glowStrength: Float,
    val scanlineShadowAlpha: Float = .48f,
    val scanlineTintAlpha: Float = .12f,
)

private fun palette(
    background: Long, panel: Long, textPrimary: Long, textSecondary: Long, textMuted: Long,
    border: Long, borderEmphasis: Long, accentPrimary: Long, accentSecondary: Long, selection: Long,
    warning: Long, error: Long, visualizerPrimary: Long, visualizerSecondary: Long, visualizerPeak: Long,
    scanlineTint: Long, placeholders: List<Long>, glowColor: Long, glowStrength: Float,
) = TerminalPalette(
    Color(background), Color(panel), Color(textPrimary), Color(textSecondary), Color(textMuted),
    Color(border), Color(borderEmphasis), Color(accentPrimary), Color(accentSecondary), Color(selection),
    Color(warning), Color(error), Color(visualizerPrimary), Color(visualizerSecondary), Color(visualizerPeak),
    Color(scanlineTint), placeholders.map(::Color), Color(glowColor), glowStrength,
)

private val GreenTerminalPalette = palette(
    0xFF000000, 0xFF050805, 0xFF00FF41, 0xFF00B32D, 0xFF008020,
    0xFF00B32D, 0xFF00FF41, 0xFFFFB000, 0xFF39FF7C, 0xFFFFB000,
    0xFFFFB000, 0xFFFF3030, 0xFF00FF41, 0xFF008020, 0xFF39FF7C,
    0xFF008020, listOf(0xFF00E676, 0xFF00BFA5, 0xFF76FF03, 0xFF64DD17), 0xFF00FF41, 0f,
)
private val AmberTerminalPalette = palette(
    0xFF000000, 0xFF0A0700, 0xFFFFB000, 0xFFD18D00, 0xFF9A6800,
    0xFFD18D00, 0xFFFFB000, 0xFFFFE082, 0xFFFFD166, 0xFFFFE082,
    0xFFFFD166, 0xFFFF4040, 0xFFFFB000, 0xFF9A6800, 0xFFFFE082,
    0xFF9A6800, listOf(0xFFFFC107, 0xFFFF9800, 0xFFFFD54F, 0xFFFFB300), 0xFFFFB000, 0f,
)
private val BlueTerminalPalette = palette(
    0xFF00040A, 0xFF020B14, 0xFF5CC8FF, 0xFF2D9FD6, 0xFF21749E,
    0xFF2D9FD6, 0xFF5CC8FF, 0xFFA9E7FF, 0xFF80D8FF, 0xFFA9E7FF,
    0xFFFFB000, 0xFFFF405A, 0xFF5CC8FF, 0xFF21749E, 0xFFA9E7FF,
    0xFF21749E, listOf(0xFF29B6F6, 0xFF00ACC1, 0xFF40C4FF, 0xFF2979FF), 0xFF5CC8FF, 0f,
)
private val SynthwavePalette = palette(
    0xFF090019, 0xFF16062B, 0xFFF6EEFF, 0xFF54E7FF, 0xFFA57BC7,
    0xFF9D4EDD, 0xFFFF4FD8, 0xFFFF4FD8, 0xFF54E7FF, 0xFFFFB347,
    0xFFFFC857, 0xFFFF4D6D, 0xFFFF4FD8, 0xFF9D4EDD, 0xFFFFB347,
    0xFF7B3FB2, listOf(0xFFFF4FD8, 0xFF54E7FF, 0xFF9D4EDD, 0xFFFFB347), 0xFFFF4FD8, 0f,
)
private val NeonPalette = palette(
    0xFF000B12, 0xFF061722, 0xFFE9FDFF, 0xFF36F1CD, 0xFF568F9A,
    0xFF167C91, 0xFF28D7FE, 0xFFFF2BD6, 0xFFB6FF00, 0xFFB6FF00,
    0xFFFFB000, 0xFFFF3864, 0xFF28D7FE, 0xFFFF2BD6, 0xFFB6FF00,
    0xFF166A78, listOf(0xFF28D7FE, 0xFFFF2BD6, 0xFFB6FF00, 0xFF36F1CD), 0xFFFF2BD6, .24f,
)

internal fun paletteFor(preset: ColorThemePreset): TerminalPalette = when (preset) {
    ColorThemePreset.GREEN_TERMINAL -> GreenTerminalPalette
    ColorThemePreset.AMBER_TERMINAL -> AmberTerminalPalette
    ColorThemePreset.BLUE_TERMINAL -> BlueTerminalPalette
    ColorThemePreset.SYNTHWAVE_80S -> SynthwavePalette
    ColorThemePreset.NEON_90S -> NeonPalette
}
