package ca.stewark.nocturnel.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import ca.stewark.nocturnel.data.entity.TrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaybackConnection(context: Context) {
    private val appContext = context.applicationContext
    private var controller: MediaController? = null
    private val future = MediaController.Builder(appContext, SessionToken(appContext, ComponentName(appContext, NocturneLPlaybackService::class.java))).buildAsync()
    private val _state = MutableStateFlow(PlaybackUiState())
    val state = _state.asStateFlow()

    init {
        future.addListener({
            controller = runCatching { future.get() }.getOrNull()?.also { player ->
                player.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refresh(player)
                    override fun onIsPlayingChanged(isPlaying: Boolean) = refresh(player)
                    override fun onPlaybackStateChanged(playbackState: Int) = refresh(player)
                })
                refresh(player)
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun play(track: TrackEntity) {
        playQueue(listOf(track), 0)
    }

    fun playQueue(tracks: List<TrackEntity>, startIndex: Int = 0) {
        val items = tracks.filter { it.status == "PLAYABLE" }.map(::itemFor)
        controller?.apply { setMediaItems(items, startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)), 0); prepare(); play() }
    }

    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled; refresh(it) } }
    fun cycleRepeat() { controller?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }; refresh(it) } }
    fun release() { MediaController.releaseFuture(future); controller = null }

    private fun itemFor(track: TrackEntity) = MediaItem.Builder().setUri(track.documentUri).setMediaId(track.relativePath)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).setAlbumTitle(track.album).build()).build()
    private fun refresh(player: Player) { _state.value = PlaybackUiState(player.currentMediaItem?.mediaMetadata?.title?.toString(), player.currentMediaItem?.mediaMetadata?.artist?.toString(), player.currentPosition, player.duration.coerceAtLeast(0), player.isPlaying, player.shuffleModeEnabled, player.repeatMode) }
}

data class PlaybackUiState(val title: String? = null, val artist: String? = null, val positionMs: Long = 0, val durationMs: Long = 0, val playing: Boolean = false, val shuffle: Boolean = false, val repeatMode: Int = Player.REPEAT_MODE_OFF)
