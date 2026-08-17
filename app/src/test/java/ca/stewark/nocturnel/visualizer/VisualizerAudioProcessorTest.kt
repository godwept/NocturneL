package ca.stewark.nocturnel.visualizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@UnstableApi
class VisualizerAudioProcessorTest {
    @Test fun teePublishesSamplesAndLeavesOutputByteIdentical() {
        val ring = PcmSampleRingBuffer(8)
        val sink = PcmAnalysisBufferSink(ring)
        sink.setCaptureEnabled(true)
        val processor = TeeAudioProcessor(sink)
        processor.configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        processor.flush()
        val input = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
            .putShort(16_384.toShort()).putShort(0.toShort())
            .putShort((-16_384).toShort()).putShort(0.toShort())
        input.flip()
        val expectedBytes = ByteArray(input.remaining()) { input.get(it) }

        processor.queueInput(input)

        val output = processor.getOutput()
        val actualBytes = ByteArray(output.remaining()) { index -> output.get(output.position() + index) }
        assertArrayEquals(expectedBytes, actualBytes)
        val samples = FloatArray(2)
        assertTrue(ring.copyLatest(samples))
        assertArrayEquals(floatArrayOf(.25f, -.25f), samples, .0001f)
    }
}
