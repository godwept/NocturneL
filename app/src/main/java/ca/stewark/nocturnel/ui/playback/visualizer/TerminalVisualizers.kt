package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
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
        VisualizerDisplayMode.TUNNEL -> "visualizer-tunnel"
        VisualizerDisplayMode.ART -> "visualizer-art"
    }
    var tunnelHistory by remember(mode, effectsEnabled) { mutableStateOf(TunnelHistory.Empty) }
    LaunchedEffect(mode, frame.frameId, frame.status, effectsEnabled) {
        tunnelHistory = updateTunnelHistory(tunnelHistory, mode, frame, effectsEnabled)
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
                    VisualizerDisplayMode.TUNNEL -> {
                        fun pathFor(layer: TunnelLayer): Path? {
                            if (layer.points.isEmpty()) return null
                            val path = Path()
                            path.moveTo(layer.points.first().x, layer.points.first().y)
                            layer.points.drop(1).forEach { path.lineTo(it.x, it.y) }
                            path.close()
                            return path
                        }
                        if (effectsEnabled) {
                            tunnelHistory.priorFrames.forEachIndexed { index, priorFrame ->
                                val alpha = .08f + index * .04f
                                tunnelGeometry(priorFrame, size.width, size.height).layers.forEach { layer ->
                                    pathFor(layer)?.let { path ->
                                        drawPath(path, PhosphorMuted.copy(alpha = alpha), style = Stroke(2f))
                                    }
                                }
                            }
                        }
                        val geometry = tunnelGeometry(frame, size.width, size.height)
                        geometry.layers.forEachIndexed { index, layer ->
                            val fraction = if (geometry.layers.size <= 1) 1f else index.toFloat() / (geometry.layers.size - 1)
                            val stroke = 1f + fraction * .75f
                            pathFor(layer)?.let { path ->
                                if (effectsEnabled) {
                                    drawPath(path, PhosphorDim.copy(alpha = .18f), style = Stroke(4f))
                                }
                                drawPath(path, Phosphor.copy(alpha = .35f + fraction * .55f), style = Stroke(stroke))
                            }
                        }
                        geometry.echoLayer?.let { echo ->
                            pathFor(echo)?.let { path ->
                                val alpha = frame.transient.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
                                drawPath(path, PhosphorBright.copy(alpha = alpha), style = Stroke(2f))
                            }
                        }
                    }
                    VisualizerDisplayMode.ART -> Unit
                }
            }
        }
        Scanlines(effectsEnabled, Modifier.matchParentSize())
    }
}
