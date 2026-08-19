package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
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

internal data class RingSpike(
    val base: VisualizerPoint,
    val tip: VisualizerPoint,
    val depth: Float,
    val length: Float,
)

internal data class RingEcho(
    val points: List<VisualizerPoint>,
    val alpha: Float,
    val scale: Float,
)

internal data class RingGeometry(
    val center: VisualizerPoint,
    val horizontalRadius: Float,
    val verticalRadius: Float,
    val basePoints: List<VisualizerPoint>,
    val spikes: List<RingSpike>,
    val echo: RingEcho?,
    val orbitPhase: Float,
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
        VisualizerPoint(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
    }
    val echo = (energyRadii.last() + frame.transient.coerceIn(0f, 1f) * .08f * diameter).coerceAtMost(diameter * .48f)
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

internal fun ringGeometry(
    frame: AudioAnalysisFrame,
    width: Float,
    height: Float,
    magnitudes: List<Float>? = null,
    echoState: RingEchoState? = null,
    effectsEnabled: Boolean = true,
): RingGeometry {
    val safeWidth = width.takeIf { it.isFinite() && it > 0f } ?: 0f
    val safeHeight = height.takeIf { it.isFinite() && it > 0f } ?: 0f
    val center = VisualizerPoint(safeWidth / 2f, safeHeight / 2f)
    val minimumDimension = min(safeWidth, safeHeight)
    val orbitPhase = Math.floorMod(frame.frameId, RING_ORBIT_PERIOD).toFloat() / RING_ORBIT_PERIOD * TWO_PI
    if (minimumDimension <= 0f) return RingGeometry(center, 0f, 0f, emptyList(), emptyList(), null, orbitPhase)

    val count = ringSpikeCount(minimumDimension)
    val low = frame.lowEnergy.sanitizedUnit()
    val mid = frame.midEnergy.sanitizedUnit()
    val high = frame.highEnergy.sanitizedUnit()
    val pulse = 1f + low * RING_BASS_PULSE
    val horizontalRadius = safeWidth * RING_HORIZONTAL_RADIUS * pulse
    val verticalRadius = safeHeight * RING_VERTICAL_RADIUS * pulse
    val sourceMagnitudes = magnitudes ?: ringMagnitudes(frame, count)
    val projectedMagnitudes = resampleCircular(sourceMagnitudes, count)
    val inset = minimumDimension * RING_SAFETY_INSET
    val basePoints = List(count) { index ->
        val angle = TWO_PI * index / count
        boundedPoint(
            center.x + cos(angle) * horizontalRadius,
            center.y + sin(angle) * verticalRadius,
            safeWidth,
            safeHeight,
            inset,
        )
    }
    val spikes = List(count) { index ->
        val angle = TWO_PI * index / count
        val base = basePoints[index]
        val gradientX = cos(angle) / horizontalRadius.coerceAtLeast(.0001f)
        val gradientY = sin(angle) / verticalRadius.coerceAtLeast(.0001f)
        val gradientLength = hypot(gradientX, gradientY).coerceAtLeast(.0001f)
        val normalX = gradientX / gradientLength
        val normalY = gradientY / gradientLength
        val magnitude = projectedMagnitudes.getOrElse(index) { 0f }.sanitizedUnit()
        val primary = minimumDimension * RING_PRIMARY_SPIKE * magnitude * (.35f + .65f * mid)
        val detail = minimumDimension * RING_HIGH_DETAIL * high * abs(sin(angle * 7f + orbitPhase * 2f))
        val length = (minimumDimension * RING_BASE_SPIKE + primary + detail).coerceAtMost(minimumDimension * RING_MAX_SPIKE)
        RingSpike(
            base = base,
            tip = boundedPoint(base.x + normalX * length, base.y + normalY * length, safeWidth, safeHeight, inset),
            depth = ((sin(angle) + 1f) / 2f).coerceIn(0f, 1f),
            length = length,
        )
    }
    val echo = if (effectsEnabled && echoState != null && echoState.age in 0..3) {
        val scale = 1f + RING_ECHO_STEP * (echoState.age + 1)
        RingEcho(
            points = List(count) { index ->
                val angle = TWO_PI * index / count
                boundedPoint(
                    center.x + cos(angle) * horizontalRadius * scale,
                    center.y + sin(angle) * verticalRadius * scale,
                    safeWidth,
                    safeHeight,
                    inset,
                )
            },
            alpha = echoState.intensity.sanitizedUnit() * RING_ECHO_ALPHA * (1f - echoState.age / 4f),
            scale = scale,
        )
    } else {
        null
    }
    return RingGeometry(center, horizontalRadius, verticalRadius, basePoints, spikes, echo, orbitPhase)
}

internal fun ringMagnitudes(frame: AudioAnalysisFrame, count: Int): List<Float> {
    if (count <= 0 || frame.waveform.isEmpty()) return List(count.coerceAtLeast(0)) { 0f }
    val phase = Math.floorMod(frame.frameId, RING_ORBIT_PERIOD).toFloat() / RING_ORBIT_PERIOD
    val raw = List(count) { index ->
        val position = (index.toFloat() / count + phase) % 1f
        circularSample(frame.waveform, position)
    }
    return List(count) { index ->
        val previous = raw[Math.floorMod(index - 1, count)]
        val current = raw[index]
        val next = raw[(index + 1) % count]
        previous * .25f + current * .50f + next * .25f
    }
}

private fun circularSample(samples: List<Float>, position: Float): Float {
    if (samples.isEmpty()) return 0f
    val scaled = position.coerceIn(0f, .999999f) * samples.size
    val lower = floor(scaled).toInt().coerceIn(0, samples.lastIndex)
    val upper = (lower + 1) % samples.size
    val fraction = scaled - lower
    val first = samples[lower].sanitizedSampleMagnitude()
    val second = samples[upper].sanitizedSampleMagnitude()
    return first + (second - first) * fraction
}

private fun resampleCircular(values: List<Float>, count: Int): List<Float> {
    if (count <= 0 || values.isEmpty()) return List(count.coerceAtLeast(0)) { 0f }
    return List(count) { index -> circularSample(values, index.toFloat() / count) }
}

private fun ringSpikeCount(minimumDimension: Float): Int = when {
    minimumDimension < RING_SMALL_THRESHOLD -> 64
    minimumDimension < RING_MEDIUM_THRESHOLD -> 80
    else -> 96
}

private fun boundedPoint(x: Float, y: Float, width: Float, height: Float, inset: Float): VisualizerPoint {
    val maxX = (width - inset).coerceAtLeast(inset)
    val maxY = (height - inset).coerceAtLeast(inset)
    return VisualizerPoint(
        (x.takeIf { it.isFinite() } ?: width / 2f).coerceIn(inset.coerceAtMost(width / 2f), maxX),
        (y.takeIf { it.isFinite() } ?: height / 2f).coerceIn(inset.coerceAtMost(height / 2f), maxY),
    )
}

private fun Float.sanitizedUnit(): Float = if (isFinite()) coerceIn(0f, 1f) else 0f
private fun Float.sanitizedSampleMagnitude(): Float = if (isFinite()) abs(coerceIn(-1f, 1f)) else 0f

private const val TWO_PI = (2.0 * PI).toFloat()
private const val RING_ORBIT_PERIOD = 1_440L
private const val RING_SMALL_THRESHOLD = 160f
private const val RING_MEDIUM_THRESHOLD = 280f
private const val RING_HORIZONTAL_RADIUS = .28f
private const val RING_VERTICAL_RADIUS = .15f
private const val RING_BASS_PULSE = .08f
private const val RING_BASE_SPIKE = .015f
private const val RING_PRIMARY_SPIKE = .13f
private const val RING_HIGH_DETAIL = .04f
private const val RING_MAX_SPIKE = .185f
private const val RING_SAFETY_INSET = .04f
private const val RING_ECHO_STEP = .03f
private const val RING_ECHO_ALPHA = .85f
