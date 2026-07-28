package ca.stewark.nocturnel.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import ca.stewark.nocturnel.data.entity.TrackEntity

class PlaybackConnection(context: Context) {
    private val appContext = context.applicationContext
    private var controller: MediaController? = null
    private val future = MediaController.Builder(appContext, SessionToken(appContext, ComponentName(appContext, NocturneLPlaybackService::class.java))).buildAsync()

    init {
        future.addListener({ controller = runCatching { future.get() }.getOrNull() }, ContextCompat.getMainExecutor(appContext))
    }

    fun play(track: TrackEntity) {
        val item = MediaItem.Builder().setUri(track.documentUri).setMediaId(track.relativePath)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).setAlbumTitle(track.album).build()).build()
        controller?.apply { setMediaItem(item); prepare(); play() }
    }

    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun release() { MediaController.releaseFuture(future); controller = null }
}
