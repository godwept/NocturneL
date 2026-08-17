package ca.stewark.nocturnel.visualizer

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

internal class Radix2Fft(private val size: Int) {
    private val real = FloatArray(size)
    private val imaginary = FloatArray(size)
    private val bitReversed = IntArray(size)

    init {
        require(size > 0 && size and (size - 1) == 0) { "FFT size must be a power of two" }
        val bits = Integer.numberOfTrailingZeros(size)
        for (index in 0 until size) bitReversed[index] = Integer.reverse(index) ushr (32 - bits)
    }

    fun magnitudes(input: FloatArray, output: FloatArray) {
        require(input.size == size)
        require(output.size == size / 2 + 1)
        for (index in 0 until size) {
            real[index] = input[bitReversed[index]]
            imaginary[index] = 0f
        }
        var length = 2
        while (length <= size) {
            val angle = -2.0 * PI / length
            val wLengthReal = cos(angle).toFloat()
            val wLengthImaginary = sin(angle).toFloat()
            var start = 0
            while (start < size) {
                var wReal = 1f
                var wImaginary = 0f
                for (offset in 0 until length / 2) {
                    val even = start + offset
                    val odd = even + length / 2
                    val oddReal = real[odd] * wReal - imaginary[odd] * wImaginary
                    val oddImaginary = real[odd] * wImaginary + imaginary[odd] * wReal
                    real[odd] = real[even] - oddReal
                    imaginary[odd] = imaginary[even] - oddImaginary
                    real[even] += oddReal
                    imaginary[even] += oddImaginary
                    val nextReal = wReal * wLengthReal - wImaginary * wLengthImaginary
                    wImaginary = wReal * wLengthImaginary + wImaginary * wLengthReal
                    wReal = nextReal
                }
                start += length
            }
            length = length shl 1
        }
        for (bin in output.indices) {
            val scale = if (bin == 0 || bin == size / 2) 1f / size else 2f / size
            output[bin] = hypot(real[bin], imaginary[bin]) * scale
        }
    }
}
