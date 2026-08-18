package ca.stewark.nocturnel.visualizer

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@UnstableApi
class AudioAnalysisRepositoryTest {
    @Test fun requiresPlaybackAndConsumerAndReturnsIdleWhenDisabled() = runTest {
        val repository = AudioAnalysisRepository(StandardTestDispatcher(testScheduler), 33)
        repository.bufferSink.flush(48_000, 1, C.ENCODING_PCM_16BIT)
        repository.setConsumerActive(true)
        runCurrent()
        assertEquals(AnalysisStatus.IDLE, repository.state.value.status)
        repository.setPlaybackActive(true)
        val pcm = ByteBuffer.allocateDirect(AudioAnalyzer.FFT_SIZE * 2).order(ByteOrder.nativeOrder())
        repeat(AudioAnalyzer.FFT_SIZE) { pcm.putShort((if (it % 2 == 0) 16_000 else -16_000).toShort()) }
        pcm.flip()
        repository.bufferSink.beginInputBuffer(0, 0)
        repository.bufferSink.handleBuffer(pcm)
        repository.bufferSink.updatePlaybackPosition(
            AudioAnalyzer.FFT_SIZE.toLong() * C.MICROS_PER_SECOND / 48_000,
        )
        advanceTimeBy(34)
        runCurrent()
        assertEquals(AnalysisStatus.ACTIVE, repository.state.value.status)
        repository.setConsumerActive(false)
        assertEquals(AnalysisStatus.IDLE, repository.state.value.status)
        repository.close()
    }

    @Test fun unsupportedFormatPublishesUnavailable() = runTest {
        val repository = AudioAnalysisRepository(StandardTestDispatcher(testScheduler), 33)
        repository.bufferSink.flush(48_000, 1, C.ENCODING_INVALID)
        repository.setPlaybackActive(true)
        repository.setConsumerActive(true)
        runCurrent()
        assertEquals(AnalysisStatus.UNAVAILABLE, repository.state.value.status)
        repository.close()
    }

    @Test fun offsetChangePublishesNextFrameWithoutRestartingLifecycle() = runTest {
        val repository = AudioAnalysisRepository(StandardTestDispatcher(testScheduler), 33)
        repository.bufferSink.flush(48_000, 1, C.ENCODING_PCM_16BIT)
        repository.setPlaybackActive(true)
        repository.setConsumerActive(true)
        runCurrent()
        val sampleCount = AudioAnalyzer.FFT_SIZE + 1_200
        val pcm = ByteBuffer.allocateDirect(sampleCount * 2).order(ByteOrder.nativeOrder())
        repeat(sampleCount) { pcm.putShort((if (it < 1_200) 4_000 else 16_000).toShort()) }
        pcm.flip()
        repository.bufferSink.beginInputBuffer(0, 0)
        repository.bufferSink.handleBuffer(pcm)
        repository.bufferSink.updatePlaybackPosition(sampleCount.toLong() * C.MICROS_PER_SECOND / 48_000)
        advanceTimeBy(34)
        runCurrent()
        val firstFrameId = repository.state.value.frameId
        assertEquals(AnalysisStatus.ACTIVE, repository.state.value.status)

        repository.setVisualizerSyncOffsetMs(25)
        advanceTimeBy(34)
        runCurrent()

        assertEquals(AnalysisStatus.ACTIVE, repository.state.value.status)
        assertTrue(repository.state.value.frameId > firstFrameId)
        repository.setVisualizerSyncOffsetMs(-500)
        advanceTimeBy(34)
        runCurrent()
        assertEquals(AnalysisStatus.ACTIVE, repository.state.value.status)
        repository.close()
    }
}
