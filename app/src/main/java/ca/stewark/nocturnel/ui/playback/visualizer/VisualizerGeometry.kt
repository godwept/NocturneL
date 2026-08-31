package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
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

internal data class FrequencyGridCell(
    val left: Float,
    val top: Float,
    val size: Float,
    val liveIntensity: Float,
    val ghostIntensity: Float,
)

internal const val FREQUENCY_GRID_DIMENSION = 30
private const val FREQUENCY_HOTSPOT_RADIUS = .18f
private const val FREQUENCY_GRID_GAP_RATIO = .24f
private const val FREQUENCY_GRID_MAX_INSET = 8f

private val frequencyGridAnchors = listOf(
    VisualizerPoint(.12f, .14f), VisualizerPoint(.32f, .10f), VisualizerPoint(.55f, .14f),
    VisualizerPoint(.78f, .11f), VisualizerPoint(.91f, .20f), VisualizerPoint(.20f, .27f),
    VisualizerPoint(.43f, .25f), VisualizerPoint(.68f, .29f), VisualizerPoint(.85f, .35f),
    VisualizerPoint(.10f, .42f), VisualizerPoint(.30f, .39f), VisualizerPoint(.55f, .43f),
    VisualizerPoint(.75f, .46f), VisualizerPoint(.92f, .50f), VisualizerPoint(.18f, .56f),
    VisualizerPoint(.40f, .55f), VisualizerPoint(.63f, .58f), VisualizerPoint(.84f, .62f),
    VisualizerPoint(.08f, .69f), VisualizerPoint(.27f, .71f), VisualizerPoint(.50f, .68f),
    VisualizerPoint(.72f, .73f), VisualizerPoint(.92f, .76f), VisualizerPoint(.16f, .84f),
    VisualizerPoint(.37f, .82f), VisualizerPoint(.58f, .87f), VisualizerPoint(.80f, .84f),
    VisualizerPoint(.48f, .32f), VisualizerPoint(.62f, .80f), VisualizerPoint(.36f, .65f),
    VisualizerPoint(.70f, .18f), VisualizerPoint(.48f, .92f),
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

internal fun frequencyGridGeometry(
    liveLevels: List<Float>,
    afterglow: List<BandAfterglow>,
    width: Float,
    height: Float,
): List<FrequencyGridCell> {
    val safeWidth = width.takeIf(Float::isFinite)?.coerceAtLeast(0f) ?: 0f
    val safeHeight = height.takeIf(Float::isFinite)?.coerceAtLeast(0f) ?: 0f
    val side = min(safeWidth, safeHeight)
    if (side <= 0f) return emptyList()

    val inset = min(FREQUENCY_GRID_MAX_INSET, side * .04f)
    val contentSide = side - inset * 2f
    if (contentSide <= 0f) return emptyList()
    val pitch = contentSide / FREQUENCY_GRID_DIMENSION
    val gap = pitch * FREQUENCY_GRID_GAP_RATIO
    val cellSize = pitch - gap
    val originX = (safeWidth - contentSide) / 2f
    val originY = (safeHeight - contentSide) / 2f
    val hasGhosts = liveLevels.isNotEmpty() && afterglow.size == liveLevels.size
    val ghostLevels = if (hasGhosts) afterglow.mapIndexed { bandIndex, retained ->
        val live = sanitizeFrequencyLevel(liveLevels[bandIndex])
        val retainedLevel = sanitizeFrequencyLevel(retained.retainedLevel)
        val alphaScale = (retained.alpha / BAND_AFTERGLOW_MAX_ALPHA).coerceIn(0f, 1f)
        (retainedLevel - live).coerceAtLeast(0f) * alphaScale
    } else emptyList()

    return List(FREQUENCY_GRID_DIMENSION * FREQUENCY_GRID_DIMENSION) { index ->
        val row = index / FREQUENCY_GRID_DIMENSION
        val column = index % FREQUENCY_GRID_DIMENSION
        val normalizedX = (column + .5f) / FREQUENCY_GRID_DIMENSION
        val normalizedY = (row + .5f) / FREQUENCY_GRID_DIMENSION
        val liveIntensity = blendedHotspotIntensity(normalizedX, normalizedY, liveLevels)
        FrequencyGridCell(
            left = originX + column * pitch + gap / 2f,
            top = originY + row * pitch + gap / 2f,
            size = cellSize,
            liveIntensity = liveIntensity,
            ghostIntensity = blendedHotspotIntensity(normalizedX, normalizedY, ghostLevels),
        )
    }
}

private fun blendedHotspotIntensity(x: Float, y: Float, levels: List<Float>): Float {
    var unlit = 1f
    val count = min(levels.size, frequencyGridAnchors.size)
    repeat(count) { index ->
        val anchor = frequencyGridAnchors[index]
        val distance = hypot(x - anchor.x, y - anchor.y)
        if (distance < FREQUENCY_HOTSPOT_RADIUS) {
            val falloff = 1f - distance / FREQUENCY_HOTSPOT_RADIUS
            val contribution = sanitizeFrequencyLevel(levels[index]) * falloff * falloff
            unlit *= 1f - contribution
        }
    }
    return (1f - unlit).coerceIn(0f, 1f)
}

private fun sanitizeFrequencyLevel(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
