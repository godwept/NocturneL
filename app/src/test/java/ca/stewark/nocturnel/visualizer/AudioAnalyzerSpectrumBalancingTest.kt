package ca.stewark.nocturnel.visualizer

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioAnalyzerSpectrumBalancingTest {
    @Test fun frequencyLiftStartsAtEightHundredHertzAndIsCapped() {
        val analyzer = AudioAnalyzer()

        assertEquals(1f, analyzer.frequencyLift(100f), .001f)
        assertEquals(1f, analyzer.frequencyLift(800f), .001f)
        assertEquals(2.828f, analyzer.frequencyLift(3_200f), .001f)
        assertEquals(4.25f, analyzer.frequencyLift(16_000f), .001f)
    }

    @Test fun frequencyLiftIsFiniteMonotonicAndBounded() {
        val analyzer = AudioAnalyzer()
        val frequencies = listOf(40f, 100f, 400f, 800f, 1_600f, 3_200f, 6_400f, 12_800f, 16_000f)
        val lifts = frequencies.map(analyzer::frequencyLift)

        assertTrue(lifts.all { it.isFinite() && it in 1f..4.25f })
        assertTrue(lifts.zipWithNext().all { (left, right) -> left <= right })
    }

    @Test fun neighborSmoothingSpreadsAnInteriorPeakWithoutMovingIt() {
        val source = FloatArray(AudioAnalyzer.BAND_COUNT).also { it[16] = 1f }
        val destination = FloatArray(AudioAnalyzer.BAND_COUNT)

        AudioAnalyzer().smoothNeighborBands(source, destination)

        assertEquals(.25f, destination[15], .001f)
        assertEquals(.5f, destination[16], .001f)
        assertEquals(.25f, destination[17], .001f)
        assertEquals(1f, destination.sum(), .001f)
    }

    @Test fun neighborSmoothingUsesNormalizedEdgeWeights() {
        val first = FloatArray(AudioAnalyzer.BAND_COUNT).also { it[0] = 1f }
        val firstResult = FloatArray(AudioAnalyzer.BAND_COUNT)
        val last = FloatArray(AudioAnalyzer.BAND_COUNT).also { it[it.lastIndex] = 1f }
        val lastResult = FloatArray(AudioAnalyzer.BAND_COUNT)
        val analyzer = AudioAnalyzer()

        analyzer.smoothNeighborBands(first, firstResult)
        analyzer.smoothNeighborBands(last, lastResult)

        assertEquals(.7f, firstResult[0], .001f)
        assertEquals(.25f, firstResult[1], .001f)
        assertEquals(.25f, lastResult[lastResult.lastIndex - 1], .001f)
        assertEquals(.7f, lastResult.last(), .001f)
    }

    @Test fun neighborSmoothingPreservesAFlatSpectrumAndSanitizesValues() {
        val analyzer = AudioAnalyzer()
        val flatResult = FloatArray(AudioAnalyzer.BAND_COUNT)
        analyzer.smoothNeighborBands(FloatArray(AudioAnalyzer.BAND_COUNT) { .4f }, flatResult)
        assertTrue(flatResult.all { kotlin.math.abs(it - .4f) < .001f })

        val invalid = FloatArray(AudioAnalyzer.BAND_COUNT) { .4f }.also {
            it[4] = Float.NaN
            it[5] = Float.POSITIVE_INFINITY
            it[6] = 2f
            it[7] = -1f
        }
        val invalidResult = FloatArray(AudioAnalyzer.BAND_COUNT)
        analyzer.smoothNeighborBands(invalid, invalidResult)
        assertTrue(invalidResult.all { it.isFinite() && it in 0f..1f })
    }

    @Test(expected = IllegalArgumentException::class)
    fun neighborSmoothingRejectsWrongSourceSize() {
        AudioAnalyzer().smoothNeighborBands(
            FloatArray(AudioAnalyzer.BAND_COUNT - 1),
            FloatArray(AudioAnalyzer.BAND_COUNT),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun neighborSmoothingRejectsWrongDestinationSize() {
        AudioAnalyzer().smoothNeighborBands(
            FloatArray(AudioAnalyzer.BAND_COUNT),
            FloatArray(AudioAnalyzer.BAND_COUNT - 1),
        )
    }

    @Test fun compensationKeepsTheRightHalfActiveForAnEqualAmplitudeMix() {
        val analyzer = AudioAnalyzer()
        val samples = mix(
            sine(400, .08f),
            sine(6_400, .08f),
        )

        val bands = analyzer.analyze(samples, 48_000).bands
        val lowBand = bandContaining(analyzer, 400f)
        val highBand = bandContaining(analyzer, 6_400f)
        val lowNeighborhood = bands.slice((lowBand - 1)..(lowBand + 1)).max()
        val highNeighborhood = bands.slice((highBand - 1)..(highBand + 1)).max()

        assertTrue(lowNeighborhood > 0f)
        assertTrue(
            "Expected compensated high neighborhood $highNeighborhood to remain visible beside low $lowNeighborhood",
            highNeighborhood >= lowNeighborhood * .5f,
        )
        val strongest = bands.indices.maxBy { bands[it] }
        assertTrue(kotlin.math.abs(strongest - lowBand) <= 1 || kotlin.math.abs(strongest - highBand) <= 1)
    }

    @Test fun spatialSmoothingRetainsTheTonePeakAndRaisesItsNeighbors() {
        val analyzer = AudioAnalyzer()
        val bands = analyzer.analyze(sine(1_000, .2f), 48_000).bands
        val target = bandContaining(analyzer, 1_000f)

        assertTrue(bands[target] == bands.max())
        assertTrue(bands[target - 1] in 0f..<bands[target])
        assertTrue(bands[target + 1] in 0f..<bands[target])
        assertTrue(bands[target - 1] > bands[target] * .1f)
        assertTrue(bands[target + 1] > bands[target] * .1f)
    }

    @Test fun sharedAdaptiveGainExpandsQuietMusicToAUsefulHeight() {
        val analyzer = AudioAnalyzer()
        val quiet = sine(1_000, .02f)

        var frame = analyzer.analyze(quiet, 48_000)
        repeat(59) { frame = analyzer.analyze(quiet, 48_000) }

        assertTrue("Expected quiet peak ${frame.bands.max()} to be expanded", frame.bands.max() in .60f..90f)
    }

    @Test fun sharedAdaptiveGainPreservesSpectralShapeAcrossLevels() {
        fun settled(amplitude: Float): List<Float> {
            val analyzer = AudioAnalyzer()
            val signal = multitone(amplitude)
            var frame = analyzer.analyze(signal, 48_000)
            repeat(59) { frame = analyzer.analyze(signal, 48_000) }
            return frame.bands
        }
        fun normalized(values: List<Float>): List<Float> {
            val maximum = values.max().coerceAtLeast(.0001f)
            return values.map { it / maximum }
        }

        val quiet = normalized(settled(.02f))
        val loud = normalized(settled(.20f))

        assertTrue(quiet.zip(loud).all { (left, right) -> kotlin.math.abs(left - right) <= .12f })
    }

    @Test fun sharedAdaptiveGainRecoversQuicklyFromAQuietToLoudTransition() {
        val analyzer = AudioAnalyzer()
        val quiet = multitone(.02f)
        repeat(60) { analyzer.analyze(quiet, 48_000) }

        val loud = multitone(.90f)
        var frame = analyzer.analyze(loud, 48_000)
        repeat(9) { frame = analyzer.analyze(loud, 48_000) }

        assertTrue(frame.bands.all { it.isFinite() && it in 0f..1f })
        assertTrue(frame.bands.count { it == 1f } < AudioAnalyzer.BAND_COUNT / 4)
    }

    @Test fun sharedAdaptiveGainDoesNotPumpAfterSettling() {
        val analyzer = AudioAnalyzer()
        val quiet = multitone(.02f)
        val peaks = List(90) { analyzer.analyze(quiet, 48_000).bands.max() }
        val latePeaks = peaks.takeLast(15)

        assertTrue(latePeaks.zipWithNext().all { (left, right) -> kotlin.math.abs(left - right) <= .03f })
    }

    @Test fun nearSilenceDoesNotRaiseAdaptiveGainOrVisibleBands() {
        val analyzer = AudioAnalyzer()
        val decoderNoise = sine(1_000, .0005f)

        var frame = analyzer.analyze(decoderNoise, 48_000)
        repeat(89) { frame = analyzer.analyze(decoderNoise, 48_000) }

        assertTrue(frame.bands.all { it == 0f })
    }

    @Test fun silenceUsesTheExistingTemporalRelease() {
        val analyzer = AudioAnalyzer()
        val signal = multitone(.02f)
        repeat(60) { analyzer.analyze(signal, 48_000) }

        val firstSilentPeak = analyzer.analyze(FloatArray(AudioAnalyzer.FFT_SIZE), 48_000).bands.max()
        val laterSilentPeaks = List(8) {
            analyzer.analyze(FloatArray(AudioAnalyzer.FFT_SIZE), 48_000).bands.max()
        }

        assertTrue(laterSilentPeaks.first() < firstSilentPeak)
        assertTrue(laterSilentPeaks.zipWithNext().all { (left, right) -> right < left })
    }

    @Test fun resetClearsAllAdaptiveHistory() {
        val reused = AudioAnalyzer()
        repeat(60) { reused.analyze(multitone(.02f), 48_000) }
        reused.reset()
        val signal = multitone(.15f)

        val afterReset = reused.analyze(signal, 48_000)
        val fresh = AudioAnalyzer().analyze(signal, 48_000)

        afterReset.bands.zip(fresh.bands).forEach { (actual, expected) ->
            assertEquals(expected, actual, .0001f)
        }
    }

    @Test fun nonFinitePcmCannotEscapeThePublishedBounds() {
        val samples = FloatArray(AudioAnalyzer.FFT_SIZE) { index ->
            when (index % 3) {
                0 -> Float.NaN
                1 -> Float.POSITIVE_INFINITY
                else -> Float.NEGATIVE_INFINITY
            }
        }

        val frame = AudioAnalyzer().analyze(samples, 48_000)
        val scalars = listOf(frame.energy, frame.lowEnergy, frame.midEnergy, frame.highEnergy, frame.transient)
        assertTrue(scalars.all { it.isFinite() && it in 0f..1f })
        assertTrue(frame.waveform.all { it.isFinite() && it in -1f..1f })
        assertTrue(frame.bands.all { it.isFinite() && it in 0f..1f })
    }

    @Test fun pinkSpectrumKeepsBothDisplayHalvesMeaningfullyActive() {
        val analyzer = AudioAnalyzer()
        val signal = pinkMultitone()
        assertTrue(signal.all { it >= -.12f && it <= .12f })

        var frame = analyzer.analyze(signal, 48_000)
        repeat(59) { frame = analyzer.analyze(signal, 48_000) }
        val left = frame.bands.take(16)
        val right = frame.bands.drop(16)
        val leftMean = left.average().toFloat()
        val rightMean = right.average().toFloat()

        assertTrue("Expected right mean $rightMean to remain visible beside left $leftMean", rightMean >= leftMean * .45f)
        assertTrue("Expected broad right-side activity: $right", right.count { it > .08f } >= 12)
        assertTrue(frame.bands.max() in .60f..90f)
        assertTrue(leftMean > rightMean)
    }

    private fun sine(frequencyHz: Int, amplitude: Float, sampleRateHz: Int = 48_000) =
        FloatArray(AudioAnalyzer.FFT_SIZE) { index ->
            (sin(2.0 * PI * frequencyHz * index / sampleRateHz) * amplitude).toFloat()
        }

    private fun mix(vararg signals: FloatArray) = FloatArray(AudioAnalyzer.FFT_SIZE) { index ->
        signals.sumOf { it[index].toDouble() }.toFloat().coerceIn(-1f, 1f)
    }

    private fun multitone(amplitude: Float) = mix(
        sine(400, amplitude),
        sine(1_000, amplitude),
        sine(6_400, amplitude),
    )

    private fun pinkMultitone(): FloatArray {
        val frequencies = intArrayOf(100, 200, 400, 800, 1_600, 3_200, 6_400, 12_800)
        val signals = frequencies.map { frequency ->
            sine(frequency, (.08 / kotlin.math.sqrt(frequency / 100.0)).toFloat())
        }.toTypedArray()
        return mix(*signals).map { it * .5f }.toFloatArray()
    }

    private fun bandContaining(analyzer: AudioAnalyzer, frequencyHz: Float): Int =
        (0 until AudioAnalyzer.BAND_COUNT).first { frequencyHz < analyzer.bandEdge(it + 1, 16_000f) }
}
