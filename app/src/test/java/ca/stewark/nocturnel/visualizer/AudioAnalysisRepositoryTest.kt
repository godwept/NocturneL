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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class, UnstableApi::class)
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
        repository.bufferSink.handleBuffer(pcm)
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
}
