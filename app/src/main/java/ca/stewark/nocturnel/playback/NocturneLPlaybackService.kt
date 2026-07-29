package ca.stewark.nocturnel.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import ca.stewark.nocturnel.NocturneLApplication
import ca.stewark.nocturnel.data.entity.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NocturneLPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var stateRepository: PlaybackStateRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var positionSaveJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        stateRepository = SharedPreferencesPlaybackStateRepository(this)
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (
                        events.containsAny(
                            Player.EVENT_MEDIA_ITEM_TRANSITION,
                            Player.EVENT_POSITION_DISCONTINUITY,
                            Player.EVENT_PLAY_WHEN_READY_CHANGED,
                            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                            Player.EVENT_REPEAT_MODE_CHANGED,
                        )
                    ) {
                        savePlaybackState()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    positionSaveJob?.cancel()
                    positionSaveJob = if (isPlaying) {
                        serviceScope.launch {
                            while (isActive) {
                                delay(POSITION_SAVE_INTERVAL_MS)
                                withContext(Dispatchers.Main) { savePlaybackState() }
                            }
                        }
                    } else {
                        null
                    }
                }
            })
        }
        mediaSession = MediaSession.Builder(this, player).build()
        restorePlaybackState()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        savePlaybackState()
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
        if (::player.isInitialized) player.release()
        super.onDestroy()
    }

    private fun restorePlaybackState() {
        val snapshot = stateRepository.load() ?: return
        serviceScope.launch {
            val app = application as NocturneLApplication
            val source = app.database.libraryDao().librarySource()
            if (source == null || source.accessLost || !app.treeAccess.canRead(source.treeUri)) {
                stateRepository.clear()
                return@launch
            }
            val tracksByPath = app.database.libraryDao().tracksByPaths(snapshot.paths).associateBy { it.relativePath }
            val restorePlan = PlaybackRestorePlanner.plan(snapshot, tracksByPath.keys)
            if (restorePlan == null) {
                stateRepository.clear()
                return@launch
            }
            val items = restorePlan.paths.mapNotNull(tracksByPath::get).map(::itemFor)
            withContext(Dispatchers.Main) {
                if (player.mediaItemCount > 0) return@withContext
                player.setMediaItems(items, restorePlan.currentIndex, restorePlan.positionMs)
                player.shuffleModeEnabled = snapshot.shuffle
                player.repeatMode = snapshot.repeat.toPlayerRepeatMode()
                player.prepare()
                player.playWhenReady = snapshot.wasPlaying
            }
        }
    }

    private fun savePlaybackState() {
        if (!::player.isInitialized || player.mediaItemCount == 0) {
            if (::stateRepository.isInitialized) stateRepository.clear()
            return
        }
        stateRepository.save(
            PlaybackSnapshot(
                paths = List(player.mediaItemCount) { player.getMediaItemAt(it).mediaId },
                currentIndex = player.currentMediaItemIndex,
                positionMs = player.currentPosition.coerceAtLeast(0),
                shuffle = player.shuffleModeEnabled,
                repeat = player.repeatMode.toRepeatMode(),
                wasPlaying = player.playWhenReady,
            ),
        )
    }

    private fun itemFor(track: TrackEntity): MediaItem =
        MediaItem.Builder()
            .setUri(track.documentUri)
            .setMediaId(track.relativePath)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setExtras(Bundle().apply { putString("album_id", track.albumId) })
                    .build(),
            )
            .build()

    private fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
    }

    private fun Int.toRepeatMode(): RepeatMode = when (this) {
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.OFF
    }

    private companion object {
        const val POSITION_SAVE_INTERVAL_MS = 5_000L
    }
}
