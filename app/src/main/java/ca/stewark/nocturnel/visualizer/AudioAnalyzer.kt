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
    private val balancedBands = FloatArray(BAND_COUNT)
    private val smoothedBands = FloatArray(BAND_COUNT)
    private val hann = FloatArray(fftSize) { index ->
        (0.5 - 0.5 * cos(2.0 * PI * index / (fftSize - 1))).toFloat()
    }
    private var previousEnergy = 0f
    private var transientValue = 0f
    private var levelEnvelope = 0f
    private var adaptiveGain = 1f
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
        val energy = sqrt(squares / fftSize).toFloat().coerceIn(0f, 1f)
        fft.magnitudes(windowed, magnitudes)
        projectBands(sampleRateHz)
        val maximumFrequency = minOf(MAX_FREQUENCY, sampleRateHz / 2f)
        for (index in rawBands.indices) {
            val centerFrequency = sqrt(
                bandEdge(index, maximumFrequency) * bandEdge(index + 1, maximumFrequency),
            )
            rawBands[index] = (rawBands[index] * frequencyLift(centerFrequency)).coerceIn(0f, 1f)
        }
        smoothNeighborBands(rawBands, balancedBands)
        if (energy < SILENCE_RMS) {
            balancedBands.fill(0f)
        } else {
            var spectralLevel = 0f
            for (value in balancedBands) spectralLevel = maxOf(spectralLevel, value)
            val levelRate = if (spectralLevel >= levelEnvelope) LEVEL_RISE else LEVEL_FALL
            levelEnvelope += (spectralLevel - levelEnvelope) * levelRate
            val targetGain = (TARGET_SPECTRUM_PEAK / maxOf(levelEnvelope, MIN_LEVEL))
                .coerceIn(MIN_ADAPTIVE_GAIN, MAX_ADAPTIVE_GAIN)
            val gainRate = if (targetGain <= adaptiveGain) GAIN_REDUCTION else GAIN_EXPANSION
            adaptiveGain += (targetGain - adaptiveGain) * gainRate
            for (index in balancedBands.indices) {
                balancedBands[index] = (balancedBands[index] * adaptiveGain).coerceIn(0f, 1f)
            }
        }
        for (index in balancedBands.indices) {
            val old = smoothedBands[index]
            val fresh = balancedBands[index]
            smoothedBands[index] = if (fresh >= old) fresh * .75f + old * .25f else fresh * .15f + old * .85f
        }
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
        balancedBands.fill(0f)
        previousEnergy = 0f
        transientValue = 0f
        levelEnvelope = 0f
        adaptiveGain = 1f
        frameId = 0
    }

    private fun projectBands(sampleRateHz: Int) {
        val maximum = minOf(MAX_FREQUENCY, sampleRateHz / 2f)
        for (band in 0 until BAND_COUNT) {
            val lower = bandEdge(band, maximum)
            val upper = bandEdge(band + 1, maximum)
            val firstBin = (lower * fftSize / sampleRateHz).toInt().coerceIn(0, magnitudes.lastIndex)
            val lastBin = (upper * fftSize / sampleRateHz).toInt().coerceIn(firstBin, magnitudes.lastIndex)
            var totalSquares = 0.0
            for (bin in firstBin..lastBin) {
                val magnitude = magnitudes[bin]
                totalSquares += magnitude * magnitude
            }
            // Total band energy avoids diluting treble bands merely because they span more FFT bins.
            val magnitude = sqrt(totalSquares).toFloat()
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

    internal fun frequencyLift(centerFrequencyHz: Float): Float {
        if (centerFrequencyHz <= FREQUENCY_LIFT_START_HZ) return 1f
        return (centerFrequencyHz / FREQUENCY_LIFT_START_HZ)
            .pow(FREQUENCY_LIFT_EXPONENT)
            .coerceAtMost(MAX_FREQUENCY_LIFT)
    }

    internal fun smoothNeighborBands(source: FloatArray, destination: FloatArray) {
        require(source.size == BAND_COUNT)
        require(destination.size == BAND_COUNT)
        fun value(index: Int) = source[index].takeIf(Float::isFinite)?.coerceIn(0f, 1f) ?: 0f
        destination[0] = value(0) * EDGE_SELF_WEIGHT + value(1) * (1f - EDGE_SELF_WEIGHT)
        for (index in 1 until BAND_COUNT - 1) {
            destination[index] = value(index - 1) * NEIGHBOR_WEIGHT +
                value(index) * CENTER_WEIGHT +
                value(index + 1) * NEIGHBOR_WEIGHT
        }
        destination[BAND_COUNT - 1] = value(BAND_COUNT - 1) * EDGE_SELF_WEIGHT +
            value(BAND_COUNT - 2) * (1f - EDGE_SELF_WEIGHT)
    }

    companion object {
        const val FFT_SIZE = 2_048
        const val WAVEFORM_POINTS = 128
        const val BAND_COUNT = 32
        private const val MIN_FREQUENCY = 40f
        private const val MAX_FREQUENCY = 16_000f
        private const val FREQUENCY_LIFT_START_HZ = 800f
        private const val FREQUENCY_LIFT_EXPONENT = .75f
        private const val MAX_FREQUENCY_LIFT = 4.25f
        private const val NEIGHBOR_WEIGHT = .25f
        private const val CENTER_WEIGHT = .50f
        private const val EDGE_SELF_WEIGHT = .70f
        private const val TARGET_SPECTRUM_PEAK = .78f
        private const val MIN_LEVEL = .0001f
        private const val MIN_ADAPTIVE_GAIN = .35f
        private const val MAX_ADAPTIVE_GAIN = 64f
        private const val LEVEL_RISE = .65f
        private const val LEVEL_FALL = .08f
        private const val GAIN_REDUCTION = .65f
        private const val GAIN_EXPANSION = .08f
        private const val SILENCE_RMS = .002f
        private val LN_NINE = ln(9f)
    }
}
