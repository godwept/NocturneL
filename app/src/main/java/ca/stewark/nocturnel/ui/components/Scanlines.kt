package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import ca.stewark.nocturnel.ui.theme.ScanlineAlpha
import ca.stewark.nocturnel.ui.theme.ScanlinePhosphorAlpha
import ca.stewark.nocturnel.ui.theme.PhosphorMuted

internal data class ScanlineStyle(
    val shadowAlpha: Float,
    val phosphorAlpha: Float,
)

internal fun scanlineStyle(enabled: Boolean): ScanlineStyle? =
    if (enabled) ScanlineStyle(ScanlineAlpha, ScanlinePhosphorAlpha) else null

@Composable
fun Scanlines(enabled: Boolean, modifier: Modifier = Modifier) {
    val style = scanlineStyle(enabled) ?: return
    Canvas(modifier.fillMaxSize().testTag("scanlines")) {
        var y = 0f
        while (y < size.height) {
            drawLine(Color.Black.copy(alpha = style.shadowAlpha), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
            // A dark stripe alone disappears over TerminalBlack; this faint phosphor line keeps CRT mode visibly distinct.
            drawLine(
                PhosphorMuted.copy(alpha = style.phosphorAlpha),
                start = androidx.compose.ui.geometry.Offset(0f, y + 1.5f),
                end = androidx.compose.ui.geometry.Offset(size.width, y + 1.5f),
                strokeWidth = 1f,
            )
            y += 3f
        }
    }
}
