package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal data class VisualizerPoint(val x: Float, val y: Float)

internal data class RadarGeometry(
    val center: VisualizerPoint,
    val gridRadii: List<Float>,
    val energyRadii: List<Float>,
    val spokeEndpoints: List<VisualizerPoint>,
    val echoRadius: Float,
    val sweepDegrees: Float,
)

internal data class SpectrumBarGeometry(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val peakY: Float,
    val segments: Int,
)

internal data class SpectrumGhostGeometry(
    val bandIndex: Int,
    val left: Float,
    val right: Float,
    val bottom: Float,
    val firstSegment: Int,
    val segments: Int,
)

internal fun radarGeometry(frame: AudioAnalysisFrame, width: Float, height: Float): RadarGeometry {
    val diameter = min(width, height)
    val center = VisualizerPoint(width / 2f, height / 2f)
    val gridRadii = listOf(.10f, .20f, .30f, .42f).map { it * diameter }
    val energyRadii = listOf(
        (.10f + frame.lowEnergy * .06f) * diameter,
        (.20f + frame.midEnergy * .06f) * diameter,
        (.30f + frame.highEnergy * .06f) * diameter,
    )
    val spokes = frame.bands.mapIndexed { index, band ->
        val angle = -PI / 2.0 + 2.0 * PI * index / frame.bands.size
        val radius = (.22f + band.coerceIn(0f, 1f) * .20f) * diameter
        VisualizerPoint(
            center.x + cos(angle).toFloat() * radius,
            center.y + sin(angle).toFloat() * radius,
        )
    }
    val echo = (energyRadii.last() + frame.transient.coerceIn(0f, 1f) * .08f * diameter)
        .coerceAtMost(diameter * .48f)
    return RadarGeometry(center, gridRadii, energyRadii, spokes, echo, (frame.frameId * 2f) % 360f)
}

internal fun radarSweepEndpoint(center: VisualizerPoint, radius: Float, sweepDegrees: Float): VisualizerPoint {
    val normalized = if (sweepDegrees.isFinite()) ((sweepDegrees % 360f) + 360f) % 360f else 0f
    val angle = (normalized - 90f) * PI / 180.0
    return VisualizerPoint(
        center.x + cos(angle).toFloat() * radius,
        center.y + sin(angle).toFloat() * radius,
    )
}

internal fun spectrumGeometry(frame: AudioAnalysisFrame, width: Float, height: Float): List<SpectrumBarGeometry> =
    spectrumGeometry(frame.bands, width, height)

internal fun spectrumGeometry(bands: List<Float>, width: Float, height: Float): List<SpectrumBarGeometry> {
    if (bands.isEmpty()) return emptyList()
    val safeWidth = width.coerceAtLeast(0f)
    val safeHeight = height.coerceAtLeast(0f)
    val inset = min(8f, min(safeWidth, safeHeight) / 2f)
    val count = bands.size
    val contentWidth = (safeWidth - inset * 2f).coerceAtLeast(0f)
    val gap = if (count > 1) min(2f, contentWidth / (count - 1)) else 0f
    val barWidth = max(0f, (contentWidth - gap * (count - 1)) / count)
    val maximumSegments = floor((safeHeight - inset * 2f).coerceAtLeast(0f) / 6f).toInt()
    return bands.mapIndexed { index, value ->
        val segments = (value.coerceIn(0f, 1f) * maximumSegments).toInt()
        val bottom = safeHeight - inset
        val top = bottom - segments * 6f
        val left = inset + index * (barWidth + gap)
        SpectrumBarGeometry(
            left,
            left + barWidth,
            top,
            bottom,
            (top - 2f).coerceIn(inset, bottom),
            segments,
        )
    }
}

internal fun spectrumGhostGeometry(
    liveLevels: List<Float>,
    retainedLevels: List<Float>,
    width: Float,
    height: Float,
): List<SpectrumGhostGeometry> {
    if (liveLevels.size != retainedLevels.size) return emptyList()
    val live = spectrumGeometry(liveLevels, width, height)
    val retained = spectrumGeometry(retainedLevels, width, height)
    return live.zip(retained).mapIndexedNotNull { index, (liveBar, retainedBar) ->
        val ghostSegments = retainedBar.segments - liveBar.segments
        if (ghostSegments <= 0) null else SpectrumGhostGeometry(
            bandIndex = index,
            left = liveBar.left,
            right = liveBar.right,
            bottom = liveBar.bottom,
            firstSegment = liveBar.segments,
            segments = ghostSegments,
        )
    }
}
