package ca.stewark.nocturnel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val NocturneLColors = darkColorScheme(
    primary = Phosphor,
    onPrimary = TerminalBlack,
    secondary = PhosphorDim,
    onSecondary = TerminalBlack,
    tertiary = AlertAmber,
    background = TerminalBlack,
    surface = TerminalBlackAlt,
    surfaceVariant = TerminalBlackAlt,
    onBackground = Phosphor,
    onSurface = Phosphor,
    onSurfaceVariant = PhosphorDim,
    error = TerminalError,
    onError = Color.Black,
    outline = Phosphor,
)

@Composable
fun NocturneLTheme(
    fontPreset: FontPreset = FontPreset.DEFAULT,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = NocturneLColors, typography = typographyFor(fontPreset)) {
        CompositionLocalProvider(LocalContentColor provides Phosphor, content = content)
    }
}
