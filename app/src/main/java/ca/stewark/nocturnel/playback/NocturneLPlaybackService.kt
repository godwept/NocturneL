package ca.stewark.nocturnel.playback

import android.os.Bundle
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import ca.stewark.nocturnel.NocturneLApplication
import ca.stewark.nocturnel.data.entity.TrackEntity
import ca.stewark.nocturnel.data.ListeningRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ca.stewark.nocturnel.visualizer.VisualizerRenderersFactory
import java.util.UUID

@UnstableApi
class NocturneLPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private lateinit var stateRepository: PlaybackStateRepository
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var positionSaveJob: Job? = null
    private var progressTracker = PlaybackProgressTracker()
    private val inFlightQualifications = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        val app = application as NocturneLApplication
        stateRepository = SharedPreferencesPlaybackStateRepository(this)
        player = ExoPlayer.Builder(this, VisualizerRenderersFactory(this, app.audioAnalysis.bufferSink)).build().apply {
            // Shuffle randomizes the visible upcoming queue once. Keep Media3's traversal linear so the
            // current item is always followed by every displayed item instead of ending mid-queue.
            setShuffleOrder(ShuffleOrder.UnshuffledShuffleOrder(0))
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            setHandleAudioBecomingNoisy(true)
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                        recordQualification(progressTracker.complete(SystemClock.elapsedRealtime()))
                    }
                    updateListeningProgress()
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
                        progressTracker.discontinuity(SystemClock.elapsedRealtime())
                    }
                    if (events.containsAny(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_IS_PLAYING_CHANGED)) {
                        updateListeningProgress()
                    }
                    if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) && player.playbackState == Player.STATE_ENDED) {
                        recordQualification(progressTracker.complete(SystemClock.elapsedRealtime()))
                    }
                    if (
                        events.containsAny(
                            Player.EVENT_MEDIA_ITEM_TRANSITION,
                            Player.EVENT_POSITION_DISCONTINUITY,
                            Player.EVENT_PLAY_WHEN_READY_CHANGED,
                            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                            Player.EVENT_REPEAT_MODE_CHANGED,
                            Player.EVENT_TIMELINE_CHANGED,
                        )
                    ) {
                        savePlaybackState()
                    }
                    if (
                        events.containsAny(
                            Player.EVENT_MEDIA_ITEM_TRANSITION,
                            Player.EVENT_POSITION_DISCONTINUITY,
                        )
                    ) {
                        app.audioAnalysis.resetStream()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    app.audioAnalysis.setPlaybackActive(isPlaying)
                    positionSaveJob?.cancel()
                    positionSaveJob = if (isPlaying) {
                        serviceScope.launch {
                            var ticks = 0
                            while (isActive) {
                                delay(PROGRESS_TICK_INTERVAL_MS)
                                withContext(Dispatchers.Main) {
                                    updateListeningProgress()
                                    ticks++
                                    if (ticks % POSITION_SAVE_TICKS == 0) savePlaybackState()
                                }
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
        (application as NocturneLApplication).audioAnalysis.setPlaybackActive(false)
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
            progressTracker = PlaybackProgressTracker(
                restorePlan.occurrences.map {
                    PlaybackOccurrenceProgress(it.relativePath, it.occurrenceId, it.accumulatedListeningMs, it.qualified)
                },
            )
            val items = restorePlan.occurrences.mapNotNull { occurrence ->
                tracksByPath[occurrence.relativePath]?.let { itemFor(it, occurrence.occurrenceId) }
            }
            withContext(Dispatchers.Main) {
                if (player.mediaItemCount > 0) return@withContext
                player.setMediaItems(items, restorePlan.currentIndex, restorePlan.positionMs)
                player.shuffleModeEnabled = snapshot.shuffle
                player.repeatMode = snapshot.repeat.toPlayerRepeatMode()
                player.prepare()
                player.playWhenReady = PlaybackRestorePolicy.shouldAutoPlay(snapshot, app.playbackSessionId)
            }
        }
    }

    private fun savePlaybackState() {
        if (!::player.isInitialized || player.mediaItemCount == 0) {
            if (::stateRepository.isInitialized) stateRepository.clear()
            return
        }
        val progressById = progressTracker.snapshot().associateBy { it.occurrenceId }
        val occurrences = List(player.mediaItemCount) { index ->
            val item = player.getMediaItemAt(index)
            val occurrenceId = item.mediaMetadata.extras?.getString(QUEUE_OCCURRENCE_ID)
                ?: "legacy:$index:${item.mediaId}"
            progressById[occurrenceId]?.let {
                PlaybackOccurrenceSnapshot(it.relativePath, it.occurrenceId, it.accumulatedListeningMs, it.qualified)
            } ?: PlaybackOccurrenceSnapshot(item.mediaId, occurrenceId)
        }
        val app = application as NocturneLApplication
        stateRepository.save(
            PlaybackSnapshot(
                paths = occurrences.map { it.relativePath },
                currentIndex = player.currentMediaItemIndex,
                positionMs = player.currentPosition.coerceAtLeast(0),
                shuffle = player.shuffleModeEnabled,
                repeat = player.repeatMode.toRepeatMode(),
                wasPlaying = player.playWhenReady,
                occurrences = occurrences,
                completed = player.playbackState == Player.STATE_ENDED,
                playbackSessionId = app.playbackSessionId,
            ),
        )
    }

    private fun updateListeningProgress() {
        val item = player.currentMediaItem ?: return
        val occurrenceId = item.mediaMetadata.extras?.getString(QUEUE_OCCURRENCE_ID) ?: return
        val duration = player.duration.takeIf { it > 0 }
            ?: item.mediaMetadata.extras?.getLong(QUEUE_DURATION_MS)?.takeIf { it > 0 }
            ?: 0
        recordQualification(
            progressTracker.update(
                occurrenceId = occurrenceId,
                relativePath = item.mediaId,
                durationMs = duration,
                isPlaying = player.isPlaying,
                nowElapsedMs = SystemClock.elapsedRealtime(),
            ),
        )
    }

    private fun recordQualification(qualification: PlaybackQualification?) {
        qualification ?: return
        if (!inFlightQualifications.add(qualification.qualificationId)) return
        serviceScope.launch {
            val result = runCatching {
                ListeningRepository((application as NocturneLApplication).database.listeningDao())
                    .recordQualifiedPlay(qualification.qualificationId, qualification.relativePath)
            }
            withContext(Dispatchers.Main) {
                inFlightQualifications -= qualification.qualificationId
                if (result.isSuccess) {
                    progressTracker.markQualified(qualification.qualificationId)
                    savePlaybackState()
                } else {
                    progressTracker.recordFailed(qualification.qualificationId)
                }
            }
        }
    }

    private fun itemFor(track: TrackEntity, occurrenceId: String = UUID.randomUUID().toString()): MediaItem =
        MediaItem.Builder()
            .setUri(track.documentUri)
            .setMediaId(track.relativePath)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setExtras(Bundle().apply {
                        putString(QUEUE_ALBUM_ID, track.albumId)
                        putString(QUEUE_OCCURRENCE_ID, occurrenceId)
                        putLong(QUEUE_DURATION_MS, track.durationMs)
                    })
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
        const val PROGRESS_TICK_INTERVAL_MS = 1_000L
        const val POSITION_SAVE_TICKS = 5
    }
}
