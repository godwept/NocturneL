package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import ca.stewark.nocturnel.ui.theme.TerminalPalette
import ca.stewark.nocturnel.ui.theme.TerminalTheme

internal data class ScanlineStyle(
    val shadowColor: Color,
    val tintColor: Color,
    val shadowAlpha: Float,
    val phosphorAlpha: Float,
)

internal fun scanlineStyle(palette: TerminalPalette, enabled: Boolean): ScanlineStyle? =
    if (enabled) ScanlineStyle(
        palette.background,
        palette.scanlineTint,
        palette.scanlineShadowAlpha,
        palette.scanlineTintAlpha,
    ) else null

@Composable
fun Scanlines(enabled: Boolean, modifier: Modifier = Modifier) {
    val style = scanlineStyle(TerminalTheme.palette, enabled) ?: return
    Canvas(modifier.fillMaxSize().testTag("scanlines")) {
        var y = 0f
        while (y < size.height) {
            drawLine(style.shadowColor.copy(alpha = style.shadowAlpha), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
            drawLine(
                style.tintColor.copy(alpha = style.phosphorAlpha),
                start = androidx.compose.ui.geometry.Offset(0f, y + 1.5f),
                end = androidx.compose.ui.geometry.Offset(size.width, y + 1.5f),
                strokeWidth = 1f,
            )
            y += 3f
        }
    }
}
