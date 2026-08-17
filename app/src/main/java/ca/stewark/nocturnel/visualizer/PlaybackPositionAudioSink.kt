package ca.stewark.nocturnel.visualizer

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer

/** Keeps captured PCM aligned with the position that AudioTrack has actually played. */
@UnstableApi
internal class PlaybackPositionAudioSink(
    sink: AudioSink,
    private val analysisSink: PcmAnalysisBufferSink,
) : ForwardingAudioSink(sink) {
    private var currentInputBuffer: ByteBuffer? = null
    private var initialBufferPosition = 0

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int,
    ): Boolean {
        if (buffer !== currentInputBuffer) {
            currentInputBuffer = buffer
            initialBufferPosition = buffer.position()
        }
        analysisSink.beginInputBuffer(
            presentationTimeUs,
            byteOffset = buffer.position() - initialBufferPosition,
        )
        val handled = super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        analysisSink.updatePlaybackPosition(getCurrentPositionUs(false))
        if (handled) currentInputBuffer = null
        return handled
    }

    override fun flush() {
        currentInputBuffer = null
        super.flush()
    }

    override fun reset() {
        currentInputBuffer = null
        super.reset()
    }
}
