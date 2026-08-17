package ca.stewark.nocturnel.visualizer

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(UnstableApi::class)
class PcmAnalysisBufferSinkTest {
    @Test fun downmixes16BitStereoWithoutAdvancingInput() {
        val ring = PcmSampleRingBuffer(8)
        val sink = PcmAnalysisBufferSink(ring)
        val input = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
            .putShort(16_384.toShort()).putShort(0.toShort()).putShort((-16_384).toShort()).putShort(0.toShort())
        input.flip()
        val original = ByteArray(input.remaining()) { input.get(it) }
        sink.flush(48_000, 2, C.ENCODING_PCM_16BIT)
        sink.setCaptureEnabled(true)
        sink.handleBuffer(input)
        val output = FloatArray(2)
        assertTrue(ring.copyLatest(output))
        assertArrayEquals(floatArrayOf(.25f, -.25f), output, .0001f)
        assertEquals(0, input.position())
        assertArrayEquals(original, ByteArray(input.remaining()) { input.get(it) })
    }

    @Test fun supportsPacked24BitAndFloatSanitization() {
        val ring = PcmSampleRingBuffer(8)
        val sink = PcmAnalysisBufferSink(ring)
        sink.setCaptureEnabled(true)
        val packed = ByteBuffer.allocateDirect(6).order(ByteOrder.nativeOrder())
        packed.put(0.toByte()).put(0.toByte()).put(0x40.toByte()).put(0.toByte()).put(0.toByte()).put(0xC0.toByte()).flip()
        sink.flush(48_000, 1, C.ENCODING_PCM_24BIT)
        sink.handleBuffer(packed)
        val packedOutput = FloatArray(2)
        assertTrue(ring.copyLatest(packedOutput))
        assertArrayEquals(floatArrayOf(.5f, -.5f), packedOutput, .0001f)

        val floats = ByteBuffer.allocateDirect(8).order(ByteOrder.nativeOrder())
        floats.putFloat(Float.NaN).putFloat(2f).flip()
        sink.flush(48_000, 1, C.ENCODING_PCM_FLOAT)
        sink.handleBuffer(floats)
        val floatOutput = FloatArray(2)
        assertTrue(ring.copyLatest(floatOutput))
        assertArrayEquals(floatArrayOf(0f, 1f), floatOutput, 0f)
    }

    @Test fun disabledAndUnsupportedFormatsWriteNothing() {
        val ring = PcmSampleRingBuffer(8)
        val sink = PcmAnalysisBufferSink(ring)
        val input = ByteBuffer.allocateDirect(2).order(ByteOrder.nativeOrder()).putShort(100.toShort())
        input.flip()
        sink.flush(48_000, 1, C.ENCODING_PCM_16BIT)
        sink.handleBuffer(input)
        assertFalse(ring.copyLatest(FloatArray(1)))
        sink.flush(48_000, 1, C.ENCODING_INVALID)
        sink.setCaptureEnabled(true)
        sink.handleBuffer(input)
        assertFalse(sink.available)
        assertFalse(ring.copyLatest(FloatArray(1)))
    }
}
