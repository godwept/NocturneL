package ca.stewark.nocturnel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import ca.stewark.nocturnel.ui.theme.Phosphor
import ca.stewark.nocturnel.ui.theme.PhosphorDim
import ca.stewark.nocturnel.ui.theme.TerminalDimensions

fun seekFraction(x: Float, width: Float): Float =
    if (width <= 0f) 0f else (x / width).coerceIn(0f, 1f)

@Composable
fun TerminalSeekBar(progress: Float, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var width by remember { mutableFloatStateOf(0f) }
    val value = progress.coerceIn(0f, 1f)
    Canvas(
        modifier.fillMaxWidth().defaultMinSize(minHeight = TerminalDimensions.minimumTouchTarget)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) }
            .onSizeChanged { width = it.width.toFloat() }
            .pointerInput(width) { detectTapGestures { onSeek(seekFraction(it.x, width)) } }
            .pointerInput(width) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onSeek(seekFraction(change.position.x, width))
                }
            },
    ) {
        val y = size.height / 2
        drawLine(PhosphorDim, Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
        drawLine(Phosphor, Offset(0f, y), Offset(size.width * value, y), strokeWidth = 3f)
        drawRect(Phosphor, topLeft = Offset((size.width * value - 4f).coerceAtLeast(0f), y - 9f), size = androidx.compose.ui.geometry.Size(8f, 18f))
    }
}
