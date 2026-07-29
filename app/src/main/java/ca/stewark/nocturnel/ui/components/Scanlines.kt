package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import ca.stewark.nocturnel.ui.theme.ScanlineAlpha

@Composable
fun Scanlines(enabled: Boolean, modifier: Modifier = Modifier) {
    if (!enabled) return
    Canvas(modifier.fillMaxSize().testTag("scanlines")) {
        var y = 0f
        while (y < size.height) {
            drawLine(Color.Black.copy(alpha = ScanlineAlpha), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
            y += 3f
        }
    }
}
