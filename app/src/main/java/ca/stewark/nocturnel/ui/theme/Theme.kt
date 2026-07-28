package ca.stewark.nocturnel.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NocturneLColors = darkColorScheme(
    primary = Phosphor,
    onPrimary = TerminalBlack,
    secondary = PhosphorDim,
    background = TerminalBlack,
    surface = TerminalPanel,
    onBackground = TerminalText,
    onSurface = TerminalText,
    error = AlertAmber,
)

@Composable
fun NocturneLTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NocturneLColors, typography = NocturneLTypography, content = content)
}
