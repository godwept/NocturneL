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

internal data class RadarExtendedBeamSegment(
    val start: VisualizerPoint,
    val end: VisualizerPoint,
    val alpha: Float,
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

private val frequencyGridPortraitAnchors = listOf(
    VisualizerPoint(.12f, .08f), VisualizerPoint(.72f, .52f), VisualizerPoint(.38f, .88f),
    VisualizerPoint(.86f, .24f), VisualizerPoint(.22f, .66f), VisualizerPoint(.58f, .12f),
    VisualizerPoint(.90f, .78f), VisualizerPoint(.44f, .36f), VisualizerPoint(.10f, .46f),
    VisualizerPoint(.68f, .92f), VisualizerPoint(.30f, .18f), VisualizerPoint(.82f, .58f),
    VisualizerPoint(.52f, .72f), VisualizerPoint(.18f, .30f), VisualizerPoint(.74f, .40f),
    VisualizerPoint(.36f, .96f), VisualizerPoint(.92f, .10f), VisualizerPoint(.26f, .80f),
    VisualizerPoint(.60f, .28f), VisualizerPoint(.08f, .60f), VisualizerPoint(.48f, .48f),
    VisualizerPoint(.78f, .86f), VisualizerPoint(.34f, .56f), VisualizerPoint(.64f, .04f),
    VisualizerPoint(.14f, .90f), VisualizerPoint(.88f, .34f), VisualizerPoint(.42f, .76f),
    VisualizerPoint(.24f, .42f), VisualizerPoint(.70f, .68f), VisualizerPoint(.54f, .20f),
    VisualizerPoint(.16f, .54f), VisualizerPoint(.56f, .84f),
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

internal fun radarViewportEndpoint(
    center: VisualizerPoint,
    width: Float,
    height: Float,
    sweepDegrees: Float,
): VisualizerPoint {
    val safeWidth = width.takeIf { it.isFinite() && it > 0f } ?: return center
    val safeHeight = height.takeIf { it.isFinite() && it > 0f } ?: return center
    if (!center.x.isFinite() || !center.y.isFinite()) return center

    val normalized = if (sweepDegrees.isFinite()) ((sweepDegrees % 360f) + 360f) % 360f else 0f
    val angle = (normalized - 90f) * PI / 180.0
    val directionX = cos(angle).toFloat()
    val directionY = sin(angle).toFloat()
    val toVerticalEdge = when {
        directionX > 0f -> (safeWidth - center.x) / directionX
        directionX < 0f -> (0f - center.x) / directionX
        else -> Float.POSITIVE_INFINITY
    }
    val toHorizontalEdge = when {
        directionY > 0f -> (safeHeight - center.y) / directionY
        directionY < 0f -> (0f - center.y) / directionY
        else -> Float.POSITIVE_INFINITY
    }
    val distance = min(toVerticalEdge, toHorizontalEdge)
    if (!distance.isFinite() || distance < 0f) return center

    return VisualizerPoint(
        (center.x + directionX * distance).coerceIn(0f, safeWidth),
        (center.y + directionY * distance).coerceIn(0f, safeHeight),
    )
}

internal fun radarExtendedBeamSegments(
    center: VisualizerPoint,
    outerRadius: Float,
    width: Float,
    height: Float,
    sweepDegrees: Float,
    startAlpha: Float,
    endAlpha: Float,
    segmentCount: Int,
): List<RadarExtendedBeamSegment> {
    if (
        !outerRadius.isFinite() || outerRadius < 0f ||
        !width.isFinite() || width <= 0f ||
        !height.isFinite() || height <= 0f ||
        !center.x.isFinite() || !center.y.isFinite() ||
        segmentCount <= 0
    ) return emptyList()

    val start = radarSweepEndpoint(center, outerRadius, sweepDegrees)
    val edge = radarViewportEndpoint(center, width, height, sweepDegrees)
    val dx = edge.x - start.x
    val dy = edge.y - start.y
    if (!dx.isFinite() || !dy.isFinite() || hypot(dx, dy) <= 0f) return emptyList()

    val safeStartAlpha = if (startAlpha.isFinite()) startAlpha.coerceIn(0f, 1f) else 0f
    val safeEndAlpha = if (endAlpha.isFinite()) endAlpha.coerceIn(0f, safeStartAlpha) else 0f

    return List(segmentCount) { index ->
        val startProgress = index.toFloat() / segmentCount
        val endProgress = (index + 1).toFloat() / segmentCount
        val fadeProgress = if (segmentCount == 1) 1f else {
            val normalized = index.toFloat() / (segmentCount - 1)
            normalized * normalized
        }
        RadarExtendedBeamSegment(
            start = VisualizerPoint(start.x + dx * startProgress, start.y + dy * startProgress),
            end = VisualizerPoint(start.x + dx * endProgress, start.y + dy * endProgress),
            alpha = safeStartAlpha + (safeEndAlpha - safeStartAlpha) * fadeProgress,
        )
    }
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
    return frequencyGridCells(
        columns = FREQUENCY_GRID_DIMENSION,
        rows = FREQUENCY_GRID_DIMENSION,
        pitch = pitch,
        gap = gap,
        cellSize = cellSize,
        originX = originX,
        originY = originY,
        liveLevels = liveLevels,
        ghostLevels = frequencyGridGhostLevels(liveLevels, afterglow),
        anchors = frequencyGridAnchors,
        hotspotYScale = 1f,
    )
}

internal fun frequencyGridPortraitGeometry(
    liveLevels: List<Float>,
    afterglow: List<BandAfterglow>,
    width: Float,
    height: Float,
): List<FrequencyGridCell> {
    val safeWidth = width.takeIf { it.isFinite() && it > 0f } ?: return emptyList()
    val safeHeight = height.takeIf { it.isFinite() && it > 0f } ?: return emptyList()
    val inset = min(FREQUENCY_GRID_MAX_INSET, min(safeWidth, safeHeight) * .04f)
    val contentWidth = safeWidth - inset * 2f
    val availableHeight = safeHeight - inset * 2f
    if (contentWidth <= 0f || availableHeight <= 0f) return emptyList()

    val pitch = contentWidth / FREQUENCY_GRID_DIMENSION
    if (!pitch.isFinite() || pitch <= 0f) return emptyList()
    val rows = floor(availableHeight / pitch).toInt()
    if (rows <= 0) return emptyList()

    val gap = pitch * FREQUENCY_GRID_GAP_RATIO
    val cellSize = pitch - gap
    val contentHeight = rows * pitch
    val originX = (safeWidth - contentWidth) / 2f
    val originY = (safeHeight - contentHeight) / 2f
    return frequencyGridCells(
        columns = FREQUENCY_GRID_DIMENSION,
        rows = rows,
        pitch = pitch,
        gap = gap,
        cellSize = cellSize,
        originX = originX,
        originY = originY,
        liveLevels = liveLevels,
        ghostLevels = frequencyGridGhostLevels(liveLevels, afterglow),
        anchors = frequencyGridPortraitAnchors,
        hotspotYScale = contentHeight / contentWidth,
    )
}

private fun frequencyGridGhostLevels(
    liveLevels: List<Float>,
    afterglow: List<BandAfterglow>,
): List<Float> {
    if (liveLevels.isEmpty() || afterglow.size != liveLevels.size) return emptyList()
    return afterglow.mapIndexed { bandIndex, retained ->
        val live = sanitizeFrequencyLevel(liveLevels[bandIndex])
        val retainedLevel = sanitizeFrequencyLevel(retained.retainedLevel)
        val alphaScale = (retained.alpha / BAND_AFTERGLOW_MAX_ALPHA).coerceIn(0f, 1f)
        (retainedLevel - live).coerceAtLeast(0f) * alphaScale
    }
}

private fun frequencyGridCells(
    columns: Int,
    rows: Int,
    pitch: Float,
    gap: Float,
    cellSize: Float,
    originX: Float,
    originY: Float,
    liveLevels: List<Float>,
    ghostLevels: List<Float>,
    anchors: List<VisualizerPoint>,
    hotspotYScale: Float,
): List<FrequencyGridCell> = List(columns * rows) { index ->
    val row = index / columns
    val column = index % columns
    val normalizedX = (column + .5f) / columns
    val normalizedY = (row + .5f) / rows
    FrequencyGridCell(
        left = originX + column * pitch + gap / 2f,
        top = originY + row * pitch + gap / 2f,
        size = cellSize,
        liveIntensity = blendedHotspotIntensity(
            normalizedX,
            normalizedY,
            liveLevels,
            anchors,
            hotspotYScale,
        ),
        ghostIntensity = blendedHotspotIntensity(
            normalizedX,
            normalizedY,
            ghostLevels,
            anchors,
            hotspotYScale,
        ),
    )
}

private fun blendedHotspotIntensity(
    x: Float,
    y: Float,
    levels: List<Float>,
    anchors: List<VisualizerPoint>,
    hotspotYScale: Float,
): Float {
    var unlit = 1f
    val count = min(levels.size, anchors.size)
    repeat(count) { index ->
        val anchor = anchors[index]
        val yScale = hotspotYScale.takeIf { it.isFinite() && it > 0f } ?: 1f
        val distance = hypot(x - anchor.x, (y - anchor.y) * yScale)
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
