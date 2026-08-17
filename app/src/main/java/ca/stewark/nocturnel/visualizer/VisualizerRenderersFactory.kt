package ca.stewark.nocturnel.visualizer

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor

@OptIn(UnstableApi::class)
internal class VisualizerRenderersFactory(
    context: Context,
    bufferSink: TeeAudioProcessor.AudioBufferSink,
) : DefaultRenderersFactory(context) {
    private val teeAudioProcessor = TeeAudioProcessor(bufferSink)

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setEnableFloatOutput(enableFloatOutput)
        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
        .setAudioProcessors(arrayOf(teeAudioProcessor))
        .build()
}
