package ca.stewark.nocturnel.visualizer

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

internal class AudioAnalyzer(private val fftSize: Int = 2_048) {
    private val fft = Radix2Fft(fftSize)
    private val windowed = FloatArray(fftSize)
    private val magnitudes = FloatArray(fftSize / 2 + 1)
    private val rawBands = FloatArray(BAND_COUNT)
    private val smoothedBands = FloatArray(BAND_COUNT)
    private val hann = FloatArray(fftSize) { index ->
        (0.5 - 0.5 * cos(2.0 * PI * index / (fftSize - 1))).toFloat()
    }
    private var previousEnergy = 0f
    private var transientValue = 0f
    private var frameId = 0L

    fun analyze(samples: FloatArray, sampleRateHz: Int): AudioAnalysisFrame {
        require(samples.size == fftSize)
        require(sampleRateHz > 0)
        var squares = 0.0
        for (index in samples.indices) {
            val sample = samples[index].takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f
            windowed[index] = sample * hann[index]
            squares += sample * sample
        }
        fft.magnitudes(windowed, magnitudes)
        projectBands(sampleRateHz)
        for (index in rawBands.indices) {
            val old = smoothedBands[index]
            val fresh = rawBands[index]
            smoothedBands[index] = if (fresh >= old) fresh * .75f + old * .25f else fresh * .15f + old * .85f
        }
        val energy = sqrt(squares / fftSize).toFloat().coerceIn(0f, 1f)
        val impulse = ((energy - previousEnergy).coerceAtLeast(0f) * 4f).coerceIn(0f, 1f)
        transientValue = maxOf(impulse, transientValue * .70f)
        previousEnergy = energy
        frameId++
        val waveform = List(WAVEFORM_POINTS) { point ->
            val index = point * (fftSize - 1) / (WAVEFORM_POINTS - 1)
            samples[index].takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f
        }
        val bands = smoothedBands.map { it.coerceIn(0f, 1f) }
        return AudioAnalysisFrame(
            waveform = waveform,
            bands = bands,
            energy = energy,
            lowEnergy = regionEnergy(bands, sampleRateHz, 40f, 250f),
            midEnergy = regionEnergy(bands, sampleRateHz, 250f, 2_000f),
            highEnergy = regionEnergy(bands, sampleRateHz, 2_000f, 16_000f),
            transient = transientValue,
            frameId = frameId,
            status = AnalysisStatus.ACTIVE,
        )
    }

    fun reset() {
        smoothedBands.fill(0f)
        rawBands.fill(0f)
        previousEnergy = 0f
        transientValue = 0f
        frameId = 0
    }

    private fun projectBands(sampleRateHz: Int) {
        val maximum = minOf(MAX_FREQUENCY, sampleRateHz / 2f)
        for (band in 0 until BAND_COUNT) {
            val lower = bandEdge(band, maximum)
            val upper = bandEdge(band + 1, maximum)
            val firstBin = (lower * fftSize / sampleRateHz).toInt().coerceIn(0, magnitudes.lastIndex)
            val lastBin = (upper * fftSize / sampleRateHz).toInt().coerceIn(firstBin, magnitudes.lastIndex)
            var total = 0f
            for (bin in firstBin..lastBin) total += magnitudes[bin]
            val magnitude = total / (lastBin - firstBin + 1)
            rawBands[band] = (ln(1f + 8f * magnitude) / LN_NINE).coerceIn(0f, 1f)
        }
    }

    private fun regionEnergy(bands: List<Float>, sampleRateHz: Int, lower: Float, upper: Float): Float {
        val maximum = minOf(MAX_FREQUENCY, sampleRateHz / 2f)
        var total = 0f
        var count = 0
        for (band in bands.indices) {
            val center = sqrt(bandEdge(band, maximum) * bandEdge(band + 1, maximum))
            if (center >= lower && center < minOf(upper, maximum + 1f)) {
                total += bands[band]
                count++
            }
        }
        return if (count == 0) 0f else (total / count).coerceIn(0f, 1f)
    }

    internal fun bandEdge(index: Int, maximum: Float): Float {
        if (maximum <= MIN_FREQUENCY) return maximum
        return MIN_FREQUENCY * (maximum / MIN_FREQUENCY).pow(index.toFloat() / BAND_COUNT)
    }

    companion object {
        const val FFT_SIZE = 2_048
        const val WAVEFORM_POINTS = 128
        const val BAND_COUNT = 32
        private const val MIN_FREQUENCY = 40f
        private const val MAX_FREQUENCY = 16_000f
        private val LN_NINE = ln(9f)
    }
}
