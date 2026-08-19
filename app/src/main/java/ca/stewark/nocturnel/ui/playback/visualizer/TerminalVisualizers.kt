package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import ca.stewark.nocturnel.ui.components.Scanlines
import ca.stewark.nocturnel.ui.theme.Phosphor
import ca.stewark.nocturnel.ui.theme.PhosphorBright
import ca.stewark.nocturnel.ui.theme.PhosphorDim
import ca.stewark.nocturnel.ui.theme.PhosphorMuted
import ca.stewark.nocturnel.ui.theme.TerminalBlack
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun TerminalVisualizerScene(
    mode: VisualizerDisplayMode,
    frame: AudioAnalysisFrame,
    effectsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val tag = when (mode) {
        VisualizerDisplayMode.RADAR -> "visualizer-radar"
        VisualizerDisplayMode.BANDS -> "visualizer-bands"
        VisualizerDisplayMode.ART -> "visualizer-art"
    }
    Box(
        modifier
            .background(TerminalBlack)
            .border(TerminalDimensions.border, Phosphor)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        if (frame.status == AnalysisStatus.UNAVAILABLE) {
            Text("SIGNAL UNAVAILABLE", color = PhosphorDim)
        } else {
            Canvas(Modifier.fillMaxSize()) {
                when (mode) {
                    VisualizerDisplayMode.RADAR -> {
                        val geometry = radarGeometry(frame, size.width, size.height)
                        geometry.gridRadii.forEach { radius ->
                            drawCircle(PhosphorMuted.copy(alpha = .55f), radius, Offset(geometry.center.x, geometry.center.y), style = Stroke(1f))
                        }
                        geometry.energyRadii.forEach { radius ->
                            drawCircle(PhosphorDim.copy(alpha = .75f), radius, Offset(geometry.center.x, geometry.center.y), style = Stroke(1.5f))
                        }
                        geometry.spokeEndpoints.forEach { point ->
                            drawLine(Phosphor, Offset(geometry.center.x, geometry.center.y), Offset(point.x, point.y), 1.5f)
                        }
                        if (frame.transient > 0f) {
                            drawCircle(PhosphorBright.copy(alpha = frame.transient), geometry.echoRadius, Offset(geometry.center.x, geometry.center.y), style = Stroke(2f))
                        }
                        val angle = (geometry.sweepDegrees - 90f) * PI / 180.0
                        drawLine(
                            PhosphorBright.copy(alpha = .8f),
                            Offset(geometry.center.x, geometry.center.y),
                            Offset(
                                geometry.center.x + cos(angle).toFloat() * geometry.gridRadii.last(),
                                geometry.center.y + sin(angle).toFloat() * geometry.gridRadii.last(),
                            ),
                            1f,
                        )
                    }
                    VisualizerDisplayMode.BANDS -> {
                        spectrumGeometry(frame, size.width, size.height).forEach { bar ->
                            repeat(bar.segments) { segment ->
                                val bottom = bar.bottom - segment * 6f
                                drawRect(
                                    Phosphor,
                                    topLeft = Offset(bar.left, bottom - 4f),
                                    size = Size(bar.right - bar.left, 4f),
                                )
                            }
                            drawLine(PhosphorBright.copy(alpha = .75f), Offset(bar.left, bar.peakY), Offset(bar.right, bar.peakY), 1f)
                        }
                    }
                    VisualizerDisplayMode.ART -> Unit
                }
            }
        }
        Scanlines(effectsEnabled, Modifier.matchParentSize())
    }
}
