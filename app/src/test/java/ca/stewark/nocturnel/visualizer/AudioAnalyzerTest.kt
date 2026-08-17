package ca.stewark.nocturnel.visualizer

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioAnalyzerTest {
    @Test fun silenceHasStableZeroShape() {
        val frame = AudioAnalyzer().analyze(FloatArray(AudioAnalyzer.FFT_SIZE), 48_000)
        assertEquals(AnalysisStatus.ACTIVE, frame.status)
        assertEquals(128, frame.waveform.size)
        assertEquals(32, frame.bands.size)
        assertEquals(0f, frame.energy, 0f)
        assertEquals(0f, frame.transient, 0f)
    }

    @Test fun tonesLandInExpectedRegions() {
        fun frame(frequency: Int) = AudioAnalyzer().analyze(
            FloatArray(AudioAnalyzer.FFT_SIZE) { sin(2 * PI * frequency * it / 48_000).toFloat() },
            48_000,
        )
        val low = frame(100)
        assertTrue(low.lowEnergy > low.midEnergy && low.lowEnergy > low.highEnergy)
        val mid = frame(1_000)
        assertTrue(mid.midEnergy > mid.lowEnergy && mid.midEnergy > mid.highEnergy)
        val high = frame(8_000)
        assertTrue(high.highEnergy > high.lowEnergy && high.highEnergy > high.midEnergy)
    }

    @Test fun transientDecaysAndResetRestartsFrameIds() {
        val analyzer = AudioAnalyzer()
        val loud = FloatArray(AudioAnalyzer.FFT_SIZE) { if (it % 2 == 0) 1f else -1f }
        val first = analyzer.analyze(loud, 48_000)
        val steady = analyzer.analyze(loud, 48_000)
        assertTrue(first.transient > steady.transient)
        assertEquals(2L, steady.frameId)
        analyzer.reset()
        assertEquals(1L, analyzer.analyze(FloatArray(AudioAnalyzer.FFT_SIZE), 48_000).frameId)
    }
}
