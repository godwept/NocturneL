package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
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

internal data class TunnelLayer(
    val points: List<VisualizerPoint>,
    val depth: Float,
)

internal data class TunnelGeometry(
    val center: VisualizerPoint,
    val layers: List<TunnelLayer>,
    val echoLayer: TunnelLayer?,
    val rotationDegrees: Float,
    val depthPhase: Float,
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

internal fun tunnelGeometry(frame: AudioAnalysisFrame, width: Float, height: Float): TunnelGeometry {
    val safeWidth = width.takeIf { it.isFinite() && it > 0f } ?: 0f
    val safeHeight = height.takeIf { it.isFinite() && it > 0f } ?: 0f
    val center = VisualizerPoint(safeWidth / 2f, safeHeight / 2f)
    val diameter = min(safeWidth, safeHeight)
    val depthPhase = Math.floorMod(frame.frameId, TUNNEL_DEPTH_PERIOD).toFloat() / TUNNEL_DEPTH_PERIOD
    val rotationDegrees = Math.floorMod(frame.frameId, TUNNEL_ROTATION_PERIOD) * TUNNEL_ROTATION_STEP_DEGREES
    if (diameter <= 0f) return TunnelGeometry(center, emptyList(), null, rotationDegrees, depthPhase)

    val layerCount = when {
        diameter < TUNNEL_SMALL_THRESHOLD -> 3
        diameter < TUNNEL_MEDIUM_THRESHOLD -> 5
        else -> 7
    }
    val low = frame.lowEnergy.sanitizedUnit()
    val mid = frame.midEnergy.sanitizedUnit()
    val high = frame.highEnergy.sanitizedUnit()
    val transient = frame.transient.sanitizedUnit()
    val waveform = foldedTunnelWaveform(frame.waveform)
    val maximumRadius = diameter * TUNNEL_MAX_RADIUS_FRACTION
    val rotationRadians = rotationDegrees * PI.toFloat() / 180f
    val layers = List(layerCount) { index ->
        val wrapped = (index + depthPhase) % layerCount
        val fraction = (wrapped / layerCount).coerceIn(0f, 1f)
        val spaced = fraction.pow(1f + low * TUNNEL_BASS_SPACING_EXPONENT)
        val radius = (
            diameter * (TUNNEL_MIN_RADIUS_FRACTION + TUNNEL_RADIUS_SPAN_FRACTION * spaced) *
                (1f + low * TUNNEL_BASS_PULSE)
            ).coerceAtMost(maximumRadius)
        TunnelLayer(
            points = tunnelLayerPoints(
                center = center,
                radius = radius,
                maximumRadius = maximumRadius,
                rotationRadians = rotationRadians,
                waveform = waveform,
                mid = mid,
                high = high,
                width = safeWidth,
                height = safeHeight,
            ),
            depth = fraction,
        )
    }.sortedBy { layer -> layer.points.firstOrNull()?.let { hypot(it.x - center.x, it.y - center.y) } ?: 0f }

    val echoLayer = if (transient > 0f && layers.isNotEmpty()) {
        val scale = 1f + transient * TUNNEL_ECHO_EXPANSION
        TunnelLayer(
            points = layers.last().points.map { point ->
                boundedTunnelPoint(
                    center = center,
                    x = center.x + (point.x - center.x) * scale,
                    y = center.y + (point.y - center.y) * scale,
                    maximumRadius = maximumRadius,
                    width = safeWidth,
                    height = safeHeight,
                )
            },
            depth = 1f,
        )
    } else {
        null
    }
    return TunnelGeometry(center, layers, echoLayer, rotationDegrees, depthPhase)
}

private fun tunnelLayerPoints(
    center: VisualizerPoint,
    radius: Float,
    maximumRadius: Float,
    rotationRadians: Float,
    waveform: FloatArray,
    mid: Float,
    high: Float,
    width: Float,
    height: Float,
): List<VisualizerPoint> {
    val corners = List(4) { side ->
        val angle = -PI.toFloat() / 4f + side * PI.toFloat() / 2f
        VisualizerPoint(cos(angle) * radius, sin(angle) * radius)
    }
    return buildList(TUNNEL_POINT_COUNT) {
        repeat(4) { side ->
            val start = corners[side]
            val end = corners[(side + 1) % 4]
            repeat(TUNNEL_EDGE_SEGMENTS) { step ->
                val fraction = step.toFloat() / TUNNEL_EDGE_SEGMENTS
                var localX = start.x + (end.x - start.x) * fraction
                var localY = start.y + (end.y - start.y) * fraction
                val localRadius = hypot(localX, localY).coerceAtLeast(.0001f)
                val cornerWeight = abs(2f * fraction - 1f)
                val waveformOffset = waveform[step] * radius * TUNNEL_WAVEFORM_DEFORMATION
                val highRipple = sin(2f * PI.toFloat() * fraction) * radius * TUNNEL_HIGH_RIPPLE * high
                val cornerPull = radius * TUNNEL_MID_CORNER_PULL * mid * cornerWeight
                val adjustedRadius = (localRadius + waveformOffset + highRipple - cornerPull)
                    .coerceIn(0f, maximumRadius)
                val radialScale = adjustedRadius / localRadius
                localX *= radialScale
                localY *= radialScale
                val rotatedX = localX * cos(rotationRadians) - localY * sin(rotationRadians)
                val rotatedY = localX * sin(rotationRadians) + localY * cos(rotationRadians)
                add(
                    boundedTunnelPoint(
                        center = center,
                        x = center.x + rotatedX,
                        y = center.y + rotatedY,
                        maximumRadius = maximumRadius,
                        width = width,
                        height = height,
                    ),
                )
            }
        }
    }
}

private fun boundedTunnelPoint(
    center: VisualizerPoint,
    x: Float,
    y: Float,
    maximumRadius: Float,
    width: Float,
    height: Float,
): VisualizerPoint {
    var dx = (x - center.x).takeIf(Float::isFinite) ?: 0f
    var dy = (y - center.y).takeIf(Float::isFinite) ?: 0f
    val distance = hypot(dx, dy)
    if (distance > maximumRadius && distance > 0f) {
        val scale = maximumRadius / distance
        dx *= scale
        dy *= scale
    }
    val inset = min(width, height) * TUNNEL_SAFETY_INSET_FRACTION
    val minX = inset.coerceAtMost(width / 2f)
    val minY = inset.coerceAtMost(height / 2f)
    return VisualizerPoint(
        (center.x + dx).coerceIn(minX, (width - minX).coerceAtLeast(minX)),
        (center.y + dy).coerceIn(minY, (height - minY).coerceAtLeast(minY)),
    )
}

private fun foldedTunnelWaveform(waveform: List<Float>): FloatArray {
    if (waveform.isEmpty()) return FloatArray(TUNNEL_EDGE_SEGMENTS + 1)
    val halfLastIndex = (waveform.lastIndex / 2f).coerceAtLeast(0f)
    val folded = FloatArray(TUNNEL_EDGE_SEGMENTS / 2 + 1) { position ->
        val fraction = position.toFloat() / (TUNNEL_EDGE_SEGMENTS / 2)
        val firstIndex = (halfLastIndex * fraction).roundToInt().coerceIn(0, waveform.lastIndex)
        val secondIndex = waveform.lastIndex - firstIndex
        (waveform[firstIndex].sanitizedSample() + waveform[secondIndex].sanitizedSample()) / 2f
    }
    return FloatArray(TUNNEL_EDGE_SEGMENTS + 1) { position ->
        folded[min(position, TUNNEL_EDGE_SEGMENTS - position)]
    }
}

private fun Float.sanitizedUnit(): Float = if (isFinite()) coerceIn(0f, 1f) else 0f

private fun Float.sanitizedSample(): Float = if (isFinite()) coerceIn(-1f, 1f) else 0f

private const val TUNNEL_EDGE_SEGMENTS = 8
private const val TUNNEL_POINT_COUNT = 32
private const val TUNNEL_DEPTH_PERIOD = 120L
private const val TUNNEL_ROTATION_PERIOD = 1_800L
private const val TUNNEL_ROTATION_STEP_DEGREES = .2f
private const val TUNNEL_SMALL_THRESHOLD = 96f
private const val TUNNEL_MEDIUM_THRESHOLD = 192f
private const val TUNNEL_MIN_RADIUS_FRACTION = .08f
private const val TUNNEL_RADIUS_SPAN_FRACTION = .34f
private const val TUNNEL_MAX_RADIUS_FRACTION = .46f
private const val TUNNEL_SAFETY_INSET_FRACTION = .04f
private const val TUNNEL_BASS_SPACING_EXPONENT = .20f
private const val TUNNEL_BASS_PULSE = .04f
private const val TUNNEL_WAVEFORM_DEFORMATION = .035f
private const val TUNNEL_MID_CORNER_PULL = .04f
private const val TUNNEL_HIGH_RIPPLE = .025f
private const val TUNNEL_ECHO_EXPANSION = .08f
