package ca.stewark.nocturnel.visualizer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioAnalysisRepository(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val frameIntervalMs: Long = 33L,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val sampleBuffer = PcmSampleRingBuffer()
    val bufferSink = PcmAnalysisBufferSink(sampleBuffer)
    private val _state = MutableStateFlow(AudioAnalysisFrame.Idle)
    val state = _state.asStateFlow()
    private var consumerActive = false
    private var playbackActive = false
    private var worker: Job? = null

    @Synchronized
    fun setConsumerActive(active: Boolean) {
        consumerActive = active
        updateWorker()
    }

    @Synchronized
    fun setPlaybackActive(active: Boolean) {
        playbackActive = active
        updateWorker()
    }

    @Synchronized
    fun resetStream() {
        sampleBuffer.reset()
        _state.value = AudioAnalysisFrame.Idle
    }

    @Synchronized
    fun close() {
        consumerActive = false
        playbackActive = false
        stopWorker()
        scope.cancel()
    }

    private fun updateWorker() {
        if (consumerActive && playbackActive) startWorker() else stopWorker()
    }

    private fun startWorker() {
        if (worker?.isActive == true) return
        bufferSink.setCaptureEnabled(true)
        worker = scope.launch {
            val analyzer = AudioAnalyzer()
            val window = FloatArray(AudioAnalyzer.FFT_SIZE)
            var lastWriteCount = -1L
            var lastGeneration = sampleBuffer.generation
            while (isActive) {
                val generation = sampleBuffer.generation
                if (generation != lastGeneration) {
                    analyzer.reset()
                    lastWriteCount = -1L
                    lastGeneration = generation
                }
                if (!bufferSink.available && bufferSink.sampleRateHz > 0) {
                    _state.value = AudioAnalysisFrame.Unavailable
                } else {
                    val writeCount = sampleBuffer.writeCount
                    if (writeCount != lastWriteCount && sampleBuffer.copyLatest(window)) {
                        _state.value = runCatching { analyzer.analyze(window, bufferSink.sampleRateHz) }
                            .getOrElse { AudioAnalysisFrame.Unavailable }
                        lastWriteCount = writeCount
                    }
                }
                delay(frameIntervalMs)
            }
        }
    }

    private fun stopWorker() {
        worker?.cancel()
        worker = null
        bufferSink.setCaptureEnabled(false)
        sampleBuffer.reset()
        _state.value = AudioAnalysisFrame.Idle
    }
}
