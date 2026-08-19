package ca.stewark.nocturnel.ui.playback.visualizer

import ca.stewark.nocturnel.visualizer.AnalysisStatus
import ca.stewark.nocturnel.visualizer.AudioAnalysisFrame
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualizerGeometryTest {
    private fun frame(
        waveform: List<Float> = List(128) { 0f }, bands: List<Float> = List(32) { 0f },
        low: Float = 0f, mid: Float = 0f, high: Float = 0f, transient: Float = 0f, id: Long = 0,
    ) = AudioAnalysisFrame(waveform, bands, 0f, low, mid, high, transient, id, AnalysisStatus.ACTIVE)

    @Test fun radarUsesAllBandsAndPcmFrameForSweep() {
        val quiet = radarGeometry(frame(id = 7), 200f, 200f)
        val raised = radarGeometry(frame(bands = List(32) { if (it == 0) 1f else 0f }, transient = 1f, id = 8), 200f, 200f)
        assertEquals(32, quiet.spokeEndpoints.size)
        assertTrue(raised.spokeEndpoints.first().y < quiet.spokeEndpoints.first().y)
        assertEquals(14f, quiet.sweepDegrees, 0f)
        assertTrue(raised.echoRadius > raised.energyRadii.last())
    }

    @Test fun spectrumHasThirtyTwoBoundedColumns() {
        val bars = spectrumGeometry(frame(bands = List(32) { it / 31f }), 320f, 200f)
        assertEquals(32, bars.size)
        assertTrue(bars.zipWithNext().all { (a, b) -> a.left < b.left && a.segments <= b.segments })
        assertTrue(bars.all { it.peakY <= it.top && it.top <= it.bottom })
    }

    @Test fun calmRingIsTiltedAdaptiveAndUsesProjectedNormals() {
        val geometry = ringGeometry(frame(), 320f, 320f)
        assertEquals(VisualizerPoint(160f, 160f), geometry.center)
        assertEquals(89.6f, geometry.horizontalRadius, .001f)
        assertEquals(48f, geometry.verticalRadius, .001f)
        assertEquals(96, geometry.spikes.size)
        assertEquals(96, geometry.basePoints.size)
        assertEquals(0f, geometry.orbitPhase, 0f)
        assertNull(geometry.echo)
        assertTrue(geometry.verticalRadius < geometry.horizontalRadius)
        geometry.spikes.forEach { spike ->
            val dx = spike.base.x - geometry.center.x
            val dy = spike.base.y - geometry.center.y
            val ellipse = dx * dx / (geometry.horizontalRadius * geometry.horizontalRadius) + dy * dy / (geometry.verticalRadius * geometry.verticalRadius)
            assertEquals(1f, ellipse, .002f)
            val gradientX = dx / (geometry.horizontalRadius * geometry.horizontalRadius)
            val gradientY = dy / (geometry.verticalRadius * geometry.verticalRadius)
            assertTrue(gradientX * (spike.tip.x - spike.base.x) + gradientY * (spike.tip.y - spike.base.y) > 0f)
        }
        assertEquals(64, ringGeometry(frame(), 120f, 120f).spikes.size)
        assertEquals(80, ringGeometry(frame(), 220f, 220f).spikes.size)
        assertEquals(96, ringGeometry(frame(), 320f, 320f).spikes.size)
        assertBounded(geometry, 320f, 320f)
    }

    @Test fun orbitIsDeterministicBoundedAndMovesWaveformEnergy() {
        val waveform = List(128) { if (it in 8..20) 1f else 0f }
        val initial = ringGeometry(frame(waveform = waveform, mid = 1f), 320f, 320f)
        assertEquals(initial, ringGeometry(frame(waveform = waveform, mid = 1f), 320f, 320f))
        val advanced = ringGeometry(frame(waveform = waveform, mid = 1f, id = 360), 320f, 320f)
        assertNotEquals(initial.orbitPhase, advanced.orbitPhase)
        assertNotEquals(initial.spikes.map { it.length }, advanced.spikes.map { it.length })
        assertEquals(initial.center, advanced.center)
        assertEquals(0f, ringGeometry(frame(id = 1_440), 320f, 320f).orbitPhase, .0001f)
        assertTrue(ringGeometry(frame(id = -1), 320f, 320f).orbitPhase in 0f..<(2f * PI.toFloat()))
        assertEquals(ringGeometry(frame(waveform = emptyList()), 200f, 200f).spikes, ringGeometry(frame(), 200f, 200f).spikes)
    }

    @Test fun ringMapsEnergyDimensionsAndPerspectiveIndependently() {
        val wave = List(128) { abs(sin(2.0 * PI * it / 24.0)).toFloat() }
        val calm = ringGeometry(frame(waveform = wave), 320f, 320f)
        val bass = ringGeometry(frame(waveform = wave, low = 1f), 320f, 320f)
        val mids = ringGeometry(frame(waveform = wave, mid = 1f), 320f, 320f)
        val highs = ringGeometry(frame(waveform = wave, high = 1f), 320f, 320f)
        assertEquals(calm.horizontalRadius * 1.08f, bass.horizontalRadius, .001f)
        assertEquals(calm.verticalRadius * 1.08f, bass.verticalRadius, .001f)
        assertEquals(calm.center, bass.center)
        assertTrue(mids.spikes.zip(calm.spikes).any { (raised, base) -> raised.length > base.length })
        assertEquals(calm.spikes.map { it.base }, highs.spikes.map { it.base })
        assertNotEquals(calm.spikes.map { it.length }, highs.spikes.map { it.length })
        assertTrue(highs.spikes.all { it.length <= 320f * .185f + .001f })
        assertTrue(highs.spikes.maxBy { it.base.y }.depth > highs.spikes.minBy { it.base.y }.depth)
    }

    @Test fun ringSanitizesMalformedAndSmallInputs() {
        val malformed = ringGeometry(frame(waveform = listOf(Float.NaN, Float.POSITIVE_INFINITY, -4f, 4f), low = Float.NaN, mid = Float.POSITIVE_INFINITY, high = -4f), 200f, 200f)
        assertBounded(malformed, 200f, 200f)
        val zero = ringGeometry(frame(), 0f, 0f)
        assertTrue(zero.spikes.isEmpty())
        assertTrue(zero.basePoints.isEmpty())
        assertNull(zero.echo)
        assertBounded(ringGeometry(frame(), 12f, 8f), 12f, 8f)
    }

    @Test fun echoExpandsFadesAndRespectsEffects() {
        assertNull(ringGeometry(frame(), 320f, 320f).echo)
        val echoes = (0..3).map { age -> ringGeometry(frame(), 320f, 320f, echoState = RingEchoState(age, 1f), effectsEnabled = true).echo!! }
        assertEquals(listOf(1.03f, 1.06f, 1.09f, 1.12f), echoes.map { it.scale })
        assertTrue(echoes.zipWithNext().all { (a, b) -> a.alpha > b.alpha })
        assertTrue(echoes.all { it.points.size == 96 })
        assertNull(ringGeometry(frame(), 320f, 320f, echoState = RingEchoState(0, 1f), effectsEnabled = false).echo)
        echoes.forEach { echo -> assertTrue(echo.points.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..320f && it.y in 0f..320f }) }
    }

    private fun assertBounded(geometry: RingGeometry, width: Float, height: Float) {
        val points = geometry.basePoints + geometry.spikes.flatMap { listOf(it.base, it.tip) } + geometry.echo.orEmptyPoints()
        assertTrue(points.all { it.x.isFinite() && it.y.isFinite() && it.x in 0f..width && it.y in 0f..height })
    }

    private fun RingEcho?.orEmptyPoints(): List<VisualizerPoint> = this?.points.orEmpty()
}
