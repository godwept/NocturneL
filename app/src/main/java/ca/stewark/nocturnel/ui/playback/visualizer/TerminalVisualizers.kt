package ca.stewark.nocturnel.ui.playback.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import ca.stewark.nocturnel.ui.components.Scanlines
import ca.stewark.nocturnel.ui.components.terminalBorder
import ca.stewark.nocturnel.ui.theme.TerminalPalette
import ca.stewark.nocturnel.ui.theme.TerminalTheme
import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame

internal const val RADAR_GRID_BLOOM_ALPHA = .16f
internal const val RADAR_GRID_BLOOM_WIDTH = 4f
internal const val RADAR_ENERGY_BLOOM_ALPHA = .22f
internal const val RADAR_ENERGY_BLOOM_WIDTH = 5f
internal const val RADAR_SPOKE_BLOOM_ALPHA = .20f
internal const val RADAR_SPOKE_BLOOM_WIDTH = 4f
internal const val RADAR_ECHO_BLOOM_MAX_ALPHA = .28f
internal const val RADAR_ECHO_BLOOM_WIDTH = 6f
internal const val RADAR_TRAIL_BLOOM_ALPHA_SCALE = .45f
internal const val RADAR_TRAIL_BLOOM_WIDTH = 5f
internal const val RADAR_SWEEP_BLOOM_ALPHA = .36f
internal const val RADAR_SWEEP_BLOOM_WIDTH = 7f

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
    val palette = TerminalTheme.palette
    val tag = when (mode) {
        VisualizerDisplayMode.RADAR -> "visualizer-radar"
        VisualizerDisplayMode.BANDS -> "visualizer-bands"
        VisualizerDisplayMode.ART -> "visualizer-art"
    }
    Box(
        modifier
            .background(palette.background)
            .terminalBorder(palette.borderEmphasis, emphasized = true)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        if (frame.status == AnalysisStatus.UNAVAILABLE) {
            Text("SIGNAL UNAVAILABLE", color = palette.textSecondary)
        } else {
            Canvas(Modifier.fillMaxSize()) {
                when (mode) {
                    VisualizerDisplayMode.RADAR -> {
                        val geometry = radarGeometry(frame, size.width, size.height)
                        if (effectsEnabled) {
                            drawRadarBloom(geometry, frame, afterglow.radar.samples, palette)
                        }
                        drawRadarCore(geometry, frame, afterglow.radar.samples, palette)
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
                                    palette.visualizerPrimary.copy(alpha = afterglow.bands[ghost.bandIndex].alpha),
                                    topLeft = Offset(ghost.left, bottom - 4f),
                                    size = Size(ghost.right - ghost.left, 4f),
                                )
                            }
                        }
                        spectrumGeometry(frame, size.width, size.height).forEach { bar ->
                            repeat(bar.segments) { segment ->
                                val bottom = bar.bottom - segment * 6f
                                drawRect(
                                    palette.visualizerPrimary,
                                    topLeft = Offset(bar.left, bottom - 4f),
                                    size = Size(bar.right - bar.left, 4f),
                                )
                            }
                            drawLine(palette.visualizerPeak.copy(alpha = .75f), Offset(bar.left, bar.peakY), Offset(bar.right, bar.peakY), 1f)
                        }
                    }
                    VisualizerDisplayMode.ART -> Unit
                }
            }
        }
        Scanlines(effectsEnabled, Modifier.matchParentSize())
    }
}

private fun DrawScope.drawRadarBloom(
    geometry: RadarGeometry,
    frame: AudioAnalysisFrame,
    samples: List<RadarAfterglowSample>,
    palette: TerminalPalette,
) {
    val center = Offset(geometry.center.x, geometry.center.y)
    val sweepRadius = geometry.gridRadii.last()

    geometry.gridRadii.forEach { radius ->
        drawCircle(
            palette.visualizerPrimary.copy(alpha = RADAR_GRID_BLOOM_ALPHA),
            radius,
            center,
            style = Stroke(RADAR_GRID_BLOOM_WIDTH),
        )
    }
    geometry.energyRadii.forEach { radius ->
        drawCircle(
            palette.visualizerPrimary.copy(alpha = RADAR_ENERGY_BLOOM_ALPHA),
            radius,
            center,
            style = Stroke(RADAR_ENERGY_BLOOM_WIDTH),
        )
    }
    geometry.spokeEndpoints.forEach { point ->
        drawLine(
            palette.visualizerPrimary.copy(alpha = RADAR_SPOKE_BLOOM_ALPHA),
            center,
            Offset(point.x, point.y),
            RADAR_SPOKE_BLOOM_WIDTH,
        )
    }
    if (frame.transient > 0f) {
        drawCircle(
            palette.visualizerPeak.copy(
                alpha = frame.transient.coerceIn(0f, 1f) * RADAR_ECHO_BLOOM_MAX_ALPHA,
            ),
            geometry.echoRadius,
            center,
            style = Stroke(RADAR_ECHO_BLOOM_WIDTH),
        )
    }
    samples.forEach { sample ->
        val endpoint = radarSweepEndpoint(geometry.center, sweepRadius, sample.angleDegrees)
        drawLine(
            palette.visualizerPrimary.copy(alpha = sample.alpha * RADAR_TRAIL_BLOOM_ALPHA_SCALE),
            center,
            Offset(endpoint.x, endpoint.y),
            RADAR_TRAIL_BLOOM_WIDTH,
        )
    }
    val endpoint = radarSweepEndpoint(geometry.center, sweepRadius, geometry.sweepDegrees)
    drawLine(
        palette.visualizerPeak.copy(alpha = RADAR_SWEEP_BLOOM_ALPHA),
        center,
        Offset(endpoint.x, endpoint.y),
        RADAR_SWEEP_BLOOM_WIDTH,
    )
}

private fun DrawScope.drawRadarCore(
    geometry: RadarGeometry,
    frame: AudioAnalysisFrame,
    samples: List<RadarAfterglowSample>,
    palette: TerminalPalette,
) {
    val center = Offset(geometry.center.x, geometry.center.y)
    val sweepRadius = geometry.gridRadii.last()

    geometry.gridRadii.forEach { radius ->
        drawCircle(palette.visualizerSecondary.copy(alpha = .55f), radius, center, style = Stroke(1f))
    }
    geometry.energyRadii.forEach { radius ->
        drawCircle(palette.textSecondary.copy(alpha = .75f), radius, center, style = Stroke(1.5f))
    }
    geometry.spokeEndpoints.forEach { point ->
        drawLine(palette.visualizerPrimary, center, Offset(point.x, point.y), 1.5f)
    }
    if (frame.transient > 0f) {
        drawCircle(
            palette.visualizerPeak.copy(alpha = frame.transient),
            geometry.echoRadius,
            center,
            style = Stroke(2f),
        )
    }
    samples.forEach { sample ->
        val endpoint = radarSweepEndpoint(geometry.center, sweepRadius, sample.angleDegrees)
        drawLine(
            palette.visualizerPrimary.copy(alpha = sample.alpha),
            center,
            Offset(endpoint.x, endpoint.y),
            1.5f,
        )
    }
    val endpoint = radarSweepEndpoint(geometry.center, sweepRadius, geometry.sweepDegrees)
    drawLine(
        palette.visualizerPeak.copy(alpha = .90f),
        center,
        Offset(endpoint.x, endpoint.y),
        1.5f,
    )
}
