package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
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

internal fun spectrumGeometry(frame: AudioAnalysisFrame, width: Float, height: Float): List<SpectrumBarGeometry> {
    val inset = 8f
    val gap = 2f
    val count = frame.bands.size
    val barWidth = ((width - inset * 2f - gap * (count - 1)) / count).coerceAtLeast(1f)
    val maximumSegments = floor((height - inset * 2f) / 6f).toInt().coerceAtLeast(1)
    return frame.bands.mapIndexed { index, value ->
        val segments = (value.coerceIn(0f, 1f) * maximumSegments).toInt()
        val bottom = height - inset
        val top = bottom - segments * 6f
        val left = inset + index * (barWidth + gap)
        SpectrumBarGeometry(left, left + barWidth, top, bottom, (top - 2f).coerceAtLeast(inset), segments)
    }
}

internal fun scopeGeometry(frame: AudioAnalysisFrame, width: Float, height: Float): List<VisualizerPoint> {
    return scopeGeometry(frame.waveform, width, height)
}

internal fun scopeGeometry(waveform: List<Float>, width: Float, height: Float): List<VisualizerPoint> {
    val inset = 8f
    val usableWidth = (width - inset * 2f).coerceAtLeast(0f)
    val amplitude = (height / 2f - inset).coerceAtLeast(0f)
    val centerY = height / 2f
    return waveform.mapIndexed { index, sample ->
        val fraction = if (waveform.size <= 1) 0f else index.toFloat() / (waveform.size - 1)
        VisualizerPoint(
            x = inset + usableWidth * fraction,
            y = (centerY - sample.coerceIn(-1f, 1f) * amplitude).coerceIn(inset, height - inset),
        )
    }
}
