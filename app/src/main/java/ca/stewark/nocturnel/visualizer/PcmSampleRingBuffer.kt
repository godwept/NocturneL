package ca.stewark.nocturnel.visualizer

import java.util.concurrent.atomic.AtomicLong

class PcmSampleRingBuffer(private val capacity: Int = 131_072) {
    private val samples = FloatArray(capacity)
    private val publishedCount = AtomicLong(0)
    private val resetAtCount = AtomicLong(0)
    private val resetGeneration = AtomicLong(0)

    init {
        require(capacity > 0)
    }

    val generation: Long get() = resetGeneration.get()
    val writeCount: Long get() = publishedCount.get()

    fun write(sample: Float) {
        val count = publishedCount.get()
        samples[(count % capacity).toInt()] = sample
        publishedCount.lazySet(count + 1)
    }

    fun copyLatest(destination: FloatArray, samplesBehind: Long = 0): Boolean {
        require(destination.size <= capacity)
        require(samplesBehind >= 0)
        val beforeGeneration = resetGeneration.get()
        val end = publishedCount.get() - samplesBehind
        if (end - resetAtCount.get() < destination.size) return false
        val start = end - destination.size
        for (index in destination.indices) {
            destination[index] = samples[((start + index) % capacity).toInt()]
        }
        val after = publishedCount.get()
        return beforeGeneration == resetGeneration.get() && after - start <= capacity
    }

    fun reset() {
        resetAtCount.set(publishedCount.get())
        resetGeneration.incrementAndGet()
    }
}
