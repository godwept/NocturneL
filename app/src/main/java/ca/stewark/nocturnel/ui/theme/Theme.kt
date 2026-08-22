package ca.stewark.nocturnel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalTerminalPalette = staticCompositionLocalOf<TerminalPalette> {
    error("TerminalPalette is only available inside NocturneLTheme")
}
private val LocalTerminalEffectsEnabled = staticCompositionLocalOf<Boolean> {
    error("Terminal effects state is only available inside NocturneLTheme")
}

object TerminalTheme {
    val palette: TerminalPalette
        @Composable @ReadOnlyComposable get() = LocalTerminalPalette.current

    val effectsEnabled: Boolean
        @Composable @ReadOnlyComposable get() = LocalTerminalEffectsEnabled.current
}

internal fun materialColorsFor(palette: TerminalPalette) = darkColorScheme(
    primary = palette.textPrimary,
    onPrimary = palette.background,
    secondary = palette.textSecondary,
    onSecondary = palette.background,
    tertiary = palette.accentPrimary,
    background = palette.background,
    surface = palette.panel,
    surfaceVariant = palette.panel,
    onBackground = palette.textPrimary,
    onSurface = palette.textPrimary,
    onSurfaceVariant = palette.textSecondary,
    error = palette.error,
    onError = palette.background,
    outline = palette.borderEmphasis,
    outlineVariant = palette.border,
)

@Composable
fun NocturneLTheme(
    fontPreset: FontPreset = FontPreset.DEFAULT,
    colorTheme: ColorThemePreset = ColorThemePreset.DEFAULT,
    effectsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(colorTheme)
    MaterialTheme(colorScheme = materialColorsFor(palette), typography = typographyFor(fontPreset)) {
        CompositionLocalProvider(
            LocalTerminalPalette provides palette,
            LocalTerminalEffectsEnabled provides effectsEnabled,
            LocalContentColor provides palette.textPrimary,
            content = content,
        )
    }
}
