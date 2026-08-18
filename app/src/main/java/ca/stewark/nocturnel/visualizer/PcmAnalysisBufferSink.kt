package ca.stewark.nocturnel.visualizer

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer

@UnstableApi
class PcmAnalysisBufferSink(private val samples: PcmSampleRingBuffer) : TeeAudioProcessor.AudioBufferSink {
    @Volatile private var captureEnabled = false
    @Volatile private var channelCount = 0
    @Volatile private var encoding = C.ENCODING_INVALID
    @Volatile private var inputBufferStartPositionUs = POSITION_UNSET
    @Volatile private var capturedThroughPositionUs = POSITION_UNSET
    @Volatile private var playbackPositionUs = POSITION_UNSET
    @Volatile private var visualizerSyncOffsetMs = VisualizerSyncOffset.DEFAULT_MS

    @Volatile var sampleRateHz: Int = 0
        private set
    @Volatile var available: Boolean = false
        private set

    fun setCaptureEnabled(enabled: Boolean) {
        captureEnabled = enabled
    }

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        this.sampleRateHz = sampleRateHz
        this.channelCount = channelCount
        this.encoding = encoding
        available = channelCount > 0 && bytesPerSample(encoding) > 0
        clearTiming()
        samples.reset()
    }

    fun beginInputBuffer(presentationTimeUs: Long, byteOffset: Int) {
        val frameSize = bytesPerSample(encoding) * channelCount
        inputBufferStartPositionUs = if (frameSize > 0 && presentationTimeUs != C.TIME_UNSET) {
            presentationTimeUs + framesToDurationUs(byteOffset / frameSize)
        } else {
            POSITION_UNSET
        }
    }

    fun updatePlaybackPosition(positionUs: Long) {
        playbackPositionUs = positionUs
    }

    fun setVisualizerSyncOffsetMs(offsetMs: Int) {
        visualizerSyncOffsetMs = VisualizerSyncOffset.clamp(offsetMs)
    }

    fun copyPlaybackAligned(destination: FloatArray): Boolean {
        val samplesBehind = adjustedSamplesBehind() ?: return false
        return samples.copyLatest(destination, samplesBehind)
    }

    fun playbackAlignedSampleCount(): Long {
        val samplesBehind = adjustedSamplesBehind() ?: return SAMPLE_COUNT_UNSET
        return samples.writeCount - samplesBehind
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        if (!captureEnabled || !available) return
        val bytes = bytesPerSample(encoding)
        val frameSize = bytes * channelCount
        var frameOffset = buffer.position()
        val end = buffer.limit() - frameSize
        while (frameOffset <= end) {
            var mono = 0f
            var channel = 0
            while (channel < channelCount) {
                mono += readSample(buffer, frameOffset + channel * bytes, encoding)
                channel++
            }
            samples.write((mono / channelCount).coerceIn(-1f, 1f))
            frameOffset += frameSize
        }
        val frameCount = (buffer.limit() - buffer.position()) / frameSize
        val bufferStartPosition = inputBufferStartPositionUs
        if (bufferStartPosition != POSITION_UNSET) {
            capturedThroughPositionUs = bufferStartPosition + framesToDurationUs(frameCount)
        }
    }

    private fun clearTiming() {
        inputBufferStartPositionUs = POSITION_UNSET
        capturedThroughPositionUs = POSITION_UNSET
        playbackPositionUs = POSITION_UNSET
    }

    private fun framesToDurationUs(frameCount: Int): Long =
        frameCount.toLong() * C.MICROS_PER_SECOND / sampleRateHz

    private fun adjustedSamplesBehind(): Long? {
        val capturedPosition = capturedThroughPositionUs
        val audiblePosition = playbackPositionUs
        if (capturedPosition == POSITION_UNSET || audiblePosition == POSITION_UNSET) return null
        val baseSamplesAhead =
            ((capturedPosition - audiblePosition).coerceAtLeast(0) * sampleRateHz) / C.MICROS_PER_SECOND
        val offsetSamples = visualizerSyncOffsetMs.toLong() * sampleRateHz / MILLIS_PER_SECOND
        return (baseSamplesAhead + offsetSamples).coerceAtLeast(0)
    }

    private fun readSample(buffer: ByteBuffer, offset: Int, encoding: Int): Float = when (encoding) {
        C.ENCODING_PCM_8BIT -> (((buffer.get(offset).toInt() and 0xff) - 128) / 128f)
        C.ENCODING_PCM_16BIT -> {
            val value = (buffer.get(offset).toInt() and 0xff) or (buffer.get(offset + 1).toInt() shl 8)
            value.toShort() / 32768f
        }
        C.ENCODING_PCM_24BIT -> {
            var value = (buffer.get(offset).toInt() and 0xff) or
                ((buffer.get(offset + 1).toInt() and 0xff) shl 8) or
                ((buffer.get(offset + 2).toInt() and 0xff) shl 16)
            if (value and 0x800000 != 0) value = value or -0x1000000
            value / 8_388_608f
        }
        C.ENCODING_PCM_32BIT -> littleEndianInt(buffer, offset) / 2_147_483_648f
        C.ENCODING_PCM_FLOAT -> Float.fromBits(littleEndianInt(buffer, offset)).takeIf(Float::isFinite)?.coerceIn(-1f, 1f) ?: 0f
        else -> 0f
    }

    private fun littleEndianInt(buffer: ByteBuffer, offset: Int): Int =
        (buffer.get(offset).toInt() and 0xff) or
            ((buffer.get(offset + 1).toInt() and 0xff) shl 8) or
            ((buffer.get(offset + 2).toInt() and 0xff) shl 16) or
            (buffer.get(offset + 3).toInt() shl 24)

    private fun bytesPerSample(encoding: Int): Int = when (encoding) {
        C.ENCODING_PCM_8BIT -> 1
        C.ENCODING_PCM_16BIT -> 2
        C.ENCODING_PCM_24BIT -> 3
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
        else -> 0
    }

    private companion object {
        const val POSITION_UNSET = Long.MIN_VALUE
        const val SAMPLE_COUNT_UNSET = -1L
        const val MILLIS_PER_SECOND = 1_000L
    }
}
