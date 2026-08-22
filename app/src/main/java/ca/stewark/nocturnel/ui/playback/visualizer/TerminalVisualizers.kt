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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import ca.stewark.nocturnel.ui.components.Scanlines
import ca.stewark.nocturnel.ui.theme.Phosphor
import ca.stewark.nocturnel.ui.theme.PhosphorBright
import ca.stewark.nocturnel.ui.theme.PhosphorDim
import ca.stewark.nocturnel.ui.theme.PhosphorMuted
import ca.stewark.nocturnel.ui.theme.TerminalBlack
import ca.stewark.nocturnel.ui.theme.TerminalDimensions
import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame

@Composable
internal fun TerminalVisualizerScene(
    mode: VisualizerDisplayMode,
    frame: AudioAnalysisFrame,
    effectsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    var afterglow by remember { mutableStateOf(VisualizerAfterglowState.Empty) }
    val latestFrame by rememberUpdatedState(frame)

    LaunchedEffect(mode, effectsEnabled, frame.status, measuredSize) {
        val eligible = mode != VisualizerDisplayMode.ART &&
            effectsEnabled &&
            frame.status == AnalysisStatus.ACTIVE &&
            measuredSize.width > 0 &&
            measuredSize.height > 0
        if (!eligible) {
            afterglow = VisualizerAfterglowState.Empty
            return@LaunchedEffect
        }

        var previousFrameNanos: Long? = null
        while (true) {
            withFrameNanos { frameNanos ->
                val elapsedNanos = previousFrameNanos
                    ?.let { (frameNanos - it).coerceAtLeast(0L) }
                    ?: 0L
                previousFrameNanos = frameNanos
                afterglow = updateVisualizerAfterglow(
                    state = afterglow,
                    mode = mode,
                    frame = latestFrame,
                    effectsEnabled = effectsEnabled,
                    size = VisualizerSizeKey(measuredSize.width, measuredSize.height),
                    elapsedNanos = elapsedNanos,
                )
            }
        }
    }

    val visibleAfterglow = if (
        effectsEnabled &&
        frame.status == AnalysisStatus.ACTIVE &&
        afterglow.activeMode == mode &&
        afterglow.size == VisualizerSizeKey(measuredSize.width, measuredSize.height)
    ) afterglow else VisualizerAfterglowState.Empty

    TerminalVisualizerFrame(
        mode = mode,
        frame = frame,
        effectsEnabled = effectsEnabled,
        afterglow = visibleAfterglow,
        modifier = modifier.onSizeChanged { measuredSize = it },
    )
}

@Composable
internal fun TerminalVisualizerFrame(
    mode: VisualizerDisplayMode,
    frame: AudioAnalysisFrame,
    effectsEnabled: Boolean,
    afterglow: VisualizerAfterglowState,
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
                        afterglow.radar.samples.forEach { sample ->
                            val endpoint = radarSweepEndpoint(geometry.center, geometry.gridRadii.last(), sample.angleDegrees)
                            drawLine(
                                PhosphorDim.copy(alpha = sample.alpha),
                                Offset(geometry.center.x, geometry.center.y),
                                Offset(endpoint.x, endpoint.y),
                                1f,
                            )
                        }
                        val endpoint = radarSweepEndpoint(geometry.center, geometry.gridRadii.last(), geometry.sweepDegrees)
                        drawLine(
                            PhosphorBright.copy(alpha = .8f),
                            Offset(geometry.center.x, geometry.center.y),
                            Offset(endpoint.x, endpoint.y),
                            1f,
                        )
                    }
                    VisualizerDisplayMode.BANDS -> {
                        spectrumGhostGeometry(
                            liveLevels = frame.bands,
                            retainedLevels = afterglow.bands.map { it.retainedLevel },
                            width = size.width,
                            height = size.height,
                        ).forEach { ghost ->
                            repeat(ghost.segments) { offset ->
                                val segment = ghost.firstSegment + offset
                                val bottom = ghost.bottom - segment * 6f
                                drawRect(
                                    PhosphorDim.copy(alpha = afterglow.bands[ghost.bandIndex].alpha),
                                    topLeft = Offset(ghost.left, bottom - 4f),
                                    size = Size(ghost.right - ghost.left, 4f),
                                )
                            }
                        }
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
