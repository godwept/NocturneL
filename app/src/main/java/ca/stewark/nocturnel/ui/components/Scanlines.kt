package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun Scanlines(enabled: Boolean) {
    if (!enabled) return
    Canvas(Modifier.fillMaxSize()) {
        var y = 0f
        while (y < size.height) { drawLine(Color.Black.copy(alpha = .22f), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f); y += 3f }
    }
}
