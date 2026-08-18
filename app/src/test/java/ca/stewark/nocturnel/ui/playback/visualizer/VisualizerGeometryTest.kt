package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerGeometryTest {
    private fun frame(
        waveform: List<Float> = List(128) { 0f },
        bands: List<Float> = List(32) { 0f },
        low: Float = 0f,
        mid: Float = 0f,
        high: Float = 0f,
        transient: Float = 0f,
        id: Long = 0,
    ) = AudioAnalysisFrame(waveform, bands, 0f, low, mid, high, transient, id, AnalysisStatus.ACTIVE)

    @Test fun radarUsesAllBandsAndPcmFrameForSweep() {
        val quiet = radarGeometry(frame(id = 7), 200f, 200f)
        val raised = radarGeometry(frame(bands = List(32) { if (it == 0) 1f else 0f }, transient = 1f, id = 8), 200f, 200f)
        assertEquals(32, quiet.spokeEndpoints.size)
        assertTrue(raised.spokeEndpoints.first().y < quiet.spokeEndpoints.first().y)
        assertEquals(14f, quiet.sweepDegrees, 0f)
        assertTrue(raised.echoRadius > raised.energyRadii.last())
        assertTrue(raised.spokeEndpoints.all { it.x in 0f..200f && it.y in 0f..200f })
    }

    @Test fun spectrumHasThirtyTwoBoundedColumns() {
        val bars = spectrumGeometry(frame(bands = List(32) { it / 31f }), 320f, 200f)
        assertEquals(32, bars.size)
        assertTrue(bars.zipWithNext().all { (a, b) -> a.left < b.left && a.segments <= b.segments })
        assertTrue(bars.all { it.peakY <= it.top && it.top <= it.bottom })
    }

    @Test fun calmTunnelHasAdaptiveBoundedFourWayLayers() {
        val geometry = tunnelGeometry(frame(), 320f, 320f)
        assertEquals(VisualizerPoint(160f, 160f), geometry.center)
        assertEquals(7, geometry.layers.size)
        assertEquals(0f, geometry.depthPhase, 0f)
        assertEquals(0f, geometry.rotationDegrees, 0f)
        assertNull(geometry.echoLayer)
        assertTrue(geometry.layers.all { it.points.size == 32 && it.depth in 0f..1f })
        assertTrue(geometry.layers.zipWithNext().all { (a, b) -> radius(a.points.first(), geometry.center) < radius(b.points.first(), geometry.center) })
        assertTrue(geometry.layers.flatMap { it.points }.all { it.x in 0f..320f && it.y in 0f..320f })
        assertTrue(geometry.layers.last().points.all { radius(it, geometry.center) <= 147.2f + .01f })
        geometry.layers.forEach { layer ->
            layer.points.forEachIndexed { index, point ->
                val rotated = VisualizerPoint(
                    geometry.center.x - (point.y - geometry.center.y),
                    geometry.center.y + (point.x - geometry.center.x),
                )
                assertPointEquals(rotated, layer.points[(index + 8) % 32])
            }
        }
        assertEquals(3, tunnelGeometry(frame(), 80f, 80f).layers.size)
        assertEquals(5, tunnelGeometry(frame(), 160f, 160f).layers.size)
        assertEquals(7, tunnelGeometry(frame(), 320f, 320f).layers.size)
    }

    @Test fun tunnelMotionIsDeterministicAndBoundedByFrameId() {
        val initial = tunnelGeometry(frame(id = 0), 240f, 240f)
        assertEquals(initial, tunnelGeometry(frame(id = 0), 240f, 240f))
        val advanced = tunnelGeometry(frame(id = 1), 240f, 240f)
        assertNotEquals(initial.depthPhase, advanced.depthPhase)
        assertNotEquals(initial.rotationDegrees, advanced.rotationDegrees)
        assertEquals(initial.layers.size, advanced.layers.size)
        assertEquals(initial.center, advanced.center)
        assertEquals(0f, tunnelGeometry(frame(id = 120), 240f, 240f).depthPhase, .0001f)
        assertEquals(0f, tunnelGeometry(frame(id = 1_800), 240f, 240f).rotationDegrees, .0001f)
        val nearDepthWrap = tunnelGeometry(frame(id = 119), 240f, 240f)
        val nearWrapRadii = nearDepthWrap.layers.map { radius(it.points.first(), nearDepthWrap.center) }
        assertTrue(nearWrapRadii.zipWithNext().all { (a, b) -> b - a > 1f })
        val negative = tunnelGeometry(frame(id = -1), 240f, 240f)
        assertTrue(negative.depthPhase in 0f..<1f)
        assertTrue(negative.rotationDegrees in 0f..<360f)
        assertTrue(advanced.layers.flatMap { it.points }.all { it.x in 0f..240f && it.y in 0f..240f })
    }

    @Test fun tunnelMapsEachSignalDimensionWithoutBreakingSymmetry() {
        val calm = tunnelGeometry(frame(), 320f, 320f)
        val bass = tunnelGeometry(frame(low = 1f), 320f, 320f)
        val mids = tunnelGeometry(frame(mid = 1f), 320f, 320f)
        val highs = tunnelGeometry(frame(high = 1f), 320f, 320f)
        val wave = tunnelGeometry(
            frame(waveform = List(128) { index -> kotlin.math.sin(2.0 * Math.PI * index / 24.0).toFloat() }),
            320f,
            320f,
        )
        assertNotEquals(calm.layers.map { it.points.first() }, bass.layers.map { it.points.first() })
        val calmOuter = calm.layers.last()
        val midOuter = mids.layers.last()
        val cornerPull = radius(calmOuter.points[0], calm.center) - radius(midOuter.points[0], mids.center)
        val midpointPull = radius(calmOuter.points[4], calm.center) - radius(midOuter.points[4], mids.center)
        assertTrue(cornerPull > midpointPull)
        assertNotEquals(calmOuter.points[2], highs.layers.last().points[2])
        assertNotEquals(calmOuter.points, wave.layers.last().points)
        listOf(highs, wave).forEach { geometry ->
            geometry.layers.forEach { layer ->
                repeat(8) { index ->
                    assertEquals(
                        radius(layer.points[index], geometry.center),
                        radius(layer.points[(index + 8) % 32], geometry.center),
                        .001f,
                    )
                }
            }
        }
        val waveLayer = wave.layers.last()
        assertEquals(
            radius(waveLayer.points[1], wave.center) - radius(calmOuter.points[1], calm.center),
            radius(waveLayer.points[7], wave.center) - radius(calmOuter.points[7], calm.center),
            .001f,
        )
    }

    @Test fun tunnelSanitizesMissingMalformedAndTinyInputs() {
        val empty = tunnelGeometry(frame(waveform = emptyList(), bands = emptyList()), 200f, 200f)
        val zero = tunnelGeometry(frame(waveform = List(128) { 0f }, bands = List(32) { 0f }), 200f, 200f)
        assertEquals(zero.layers, empty.layers)
        val malformed = tunnelGeometry(
            frame(
                waveform = listOf(Float.NaN, Float.POSITIVE_INFINITY, -4f, 4f),
                low = Float.NaN,
                mid = Float.POSITIVE_INFINITY,
                high = -4f,
                transient = Float.POSITIVE_INFINITY,
            ),
            200f,
            200f,
        )
        assertTrue(malformed.layers.flatMap { it.points }.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..200f && it.y in 0f..200f })
        val zeroCanvas = tunnelGeometry(frame(transient = 1f), 0f, 0f)
        assertTrue(zeroCanvas.layers.isEmpty())
        assertNull(zeroCanvas.echoLayer)
        val tiny = tunnelGeometry(frame(), 12f, 8f)
        assertEquals(3, tiny.layers.size)
        assertTrue(tiny.layers.flatMap { it.points }.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..12f && it.y in 0f..8f })
    }

    @Test fun tunnelCreatesOneBoundedTransientEcho() {
        assertNull(tunnelGeometry(frame(transient = 0f), 320f, 320f).echoLayer)
        val low = tunnelGeometry(frame(transient = .25f), 320f, 320f)
        val high = tunnelGeometry(frame(transient = 1f), 320f, 320f)
        assertNotNull(low.echoLayer)
        assertNotNull(high.echoLayer)
        assertEquals(32, high.echoLayer!!.points.size)
        assertTrue(radius(high.echoLayer.points.first(), high.center) >= radius(low.echoLayer!!.points.first(), low.center))
        assertTrue(high.echoLayer.points.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..320f && it.y in 0f..320f })
        assertTrue(high.echoLayer.points.all { radius(it, high.center) <= 147.2f + .01f })
        assertFalse(high.layers.isEmpty())
    }

    private fun radius(point: VisualizerPoint, center: VisualizerPoint): Float =
        kotlin.math.hypot(point.x - center.x, point.y - center.y)

    private fun assertPointEquals(expected: VisualizerPoint, actual: VisualizerPoint) {
        assertEquals(expected.x, actual.x, .001f)
        assertEquals(expected.y, actual.y, .001f)
    }
}
