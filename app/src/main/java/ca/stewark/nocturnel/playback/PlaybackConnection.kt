@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package ca.stewark.nocturnel.playback

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.core.content.ContextCompat
import ca.stewark.nocturnel.NocturneLApplication
import ca.stewark.nocturnel.data.entity.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.UUID

class PlaybackConnection(context: Context) {
    private val appContext = context.applicationContext
    private val app = appContext as NocturneLApplication
    private val playbackStateRepository: PlaybackStateRepository = SharedPreferencesPlaybackStateRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var pendingQueue: Pair<List<TrackEntity>, Int>? = null
    private val pendingQueueActions = PendingQueueActions<PendingQueueAction>()
    private var pendingRemoval: PendingRemoval? = null
    private val future = MediaController.Builder(appContext, SessionToken(appContext, ComponentName(appContext, NocturneLPlaybackService::class.java))).buildAsync()
    private val _state = MutableStateFlow(PlaybackUiState())
    val state = _state.asStateFlow()
    val analysisState = app.audioAnalysis.state

    init {
        scope.launch {
            while (isActive) {
                delay(1_000)
                controller?.let(::refresh)
            }
        }
        future.addListener({
            val connected = runCatching { future.get() }.getOrNull()
            controller = connected
            connected?.also { player ->
                player.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = refresh(player)
                    override fun onIsPlayingChanged(isPlaying: Boolean) = refresh(player)
                    override fun onPlaybackStateChanged(playbackState: Int) = refresh(player)
                    override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) = refresh(player)
                    override fun onRepeatModeChanged(repeatMode: Int) = refresh(player)
                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = refresh(player)
                })
                refresh(player)
                pendingQueue?.let { (tracks, startIndex) ->
                    pendingQueue = null
                    playQueue(tracks, startIndex)
                }
                pendingQueueActions.drain().forEach(::applyPendingAction)
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun play(track: TrackEntity) {
        playQueue(listOf(track), 0)
    }

    fun playQueue(tracks: List<TrackEntity>, startIndex: Int = 0) {
        scope.launch {
            val canPlay = canAccessLibrary()
            if (!canPlay) {
                _state.value = _state.value.copy(error = "Access to the selected music folder was lost.")
                return@launch
            }
            playValidatedQueue(tracks, startIndex)
        }
    }

    private fun playValidatedQueue(tracks: List<TrackEntity>, startIndex: Int) {
        val player = controller
        if (player == null) {
            pendingQueue = tracks to startIndex
            return
        }
        val playableTracks = tracks.filter { it.status == "PLAYABLE" }
        if (playableTracks.isEmpty()) {
            _state.value = _state.value.copy(error = "No playable tracks are available in this queue.")
            return
        }
        _state.value = _state.value.copy(
            error = if (playableTracks.size < tracks.size) "Unavailable tracks were skipped." else null,
        )
        val requestedPath = tracks.getOrNull(startIndex)?.relativePath
        val playableStartIndex = playableTracks.indexOfFirst { it.relativePath == requestedPath }.takeIf { it >= 0 } ?: 0
        player.apply {
            setMediaItems(playableTracks.map(::itemFor), playableStartIndex, 0)
            prepare()
            play()
        }
    }

    fun playNext(tracks: List<TrackEntity>, skippedCount: Int = 0) = enqueue(tracks, skippedCount, QueueAddMode.NEXT)

    fun addToQueue(tracks: List<TrackEntity>, skippedCount: Int = 0) = enqueue(tracks, skippedCount, QueueAddMode.APPEND)

    private fun enqueue(tracks: List<TrackEntity>, skippedCount: Int, mode: QueueAddMode) {
        scope.launch {
            if (!canAccessLibrary()) {
                setQueueNotice("ACCESS TO THE SELECTED MUSIC FOLDER WAS LOST")
                return@launch
            }
            val action = PendingQueueAction(mode, tracks, skippedCount)
            if (controller == null) pendingQueueActions.add(action) else applyPendingAction(action)
        }
    }

    private fun applyPendingAction(action: PendingQueueAction) {
        val player = controller ?: run { pendingQueueActions.add(action); return }
        pendingRemoval = null
        val addition = queueAddition(action.tracks, action.skippedCount)
        if (addition.tracks.isEmpty()) {
            setQueueNotice(addition.message)
            return
        }
        val mediaItems = addition.tracks.map(::itemFor)
        val entries = mediaItems.map(::entryFor)
        val command = when (action.mode) {
            QueueAddMode.NEXT -> QueueEditCommand.InsertNext(entries)
            QueueAddMode.APPEND -> QueueEditCommand.Append(entries)
        }
        val result = QueueEditingPolicy.apply(snapshot(player), command)
        applyResult(player, result, mediaItems.associateBy(::occurrenceId), addition.message)
    }

    fun jumpToQueueOccurrence(occurrenceId: String) {
        val player = controller ?: return
        pendingRemoval = null
        val index = indexOfOccurrence(player, occurrenceId)
        if (index <= player.currentMediaItemIndex) {
            setQueueNotice("QUEUE CHANGED · TRY AGAIN")
            return
        }
        player.seekToDefaultPosition(index)
        refresh(player)
    }

    fun moveQueueOccurrence(occurrenceId: String, targetUpcomingIndex: Int, expectedCurrentOccurrenceId: String?) {
        val player = controller ?: return
        pendingRemoval = null
        if (currentOccurrenceId(player) != expectedCurrentOccurrenceId) {
            setQueueNotice("QUEUE CHANGED · TRY AGAIN")
            return
        }
        applyResult(player, QueueEditingPolicy.apply(snapshot(player), QueueEditCommand.Move(occurrenceId, targetUpcomingIndex)))
    }

    fun removeQueueOccurrence(occurrenceId: String) {
        val player = controller ?: return
        pendingRemoval = null
        val index = indexOfOccurrence(player, occurrenceId)
        if (index <= player.currentMediaItemIndex) {
            setQueueNotice("QUEUE CHANGED · TRY AGAIN")
            return
        }
        val removedItem = player.getMediaItemAt(index)
        val result = QueueEditingPolicy.apply(snapshot(player), QueueEditCommand.Remove(occurrenceId))
        if (applyResult(player, result)) {
            pendingRemoval = result.removed?.let { PendingRemoval(it, removedItem) }
            refresh(player)
        }
    }

    fun undoQueueRemoval() {
        val removal = pendingRemoval ?: return
        val player = controller ?: return
        val result = QueueEditingPolicy.apply(snapshot(player), QueueEditCommand.RestoreRemoved(removal.token))
        if (applyResult(player, result, mapOf(removal.token.entry.occurrenceId to removal.mediaItem))) {
            pendingRemoval = null
            refresh(player)
        }
    }

    fun clearUpcomingQueue() {
        val player = controller ?: return
        pendingRemoval = null
        applyResult(player, QueueEditingPolicy.apply(snapshot(player), QueueEditCommand.ClearUpcoming))
    }

    fun expireQueueUndo() {
        pendingRemoval = null
        controller?.let(::refresh)
    }

    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }
    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled; refresh(it) } }
    fun cycleRepeat() { controller?.let { it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }; refresh(it) } }
    fun setVisualizerActive(active: Boolean) { app.audioAnalysis.setConsumerActive(active) }
    fun setVisualizerSyncOffsetMs(offsetMs: Int) { app.audioAnalysis.setVisualizerSyncOffsetMs(offsetMs) }
    fun release() { pendingQueue = null; pendingQueueActions.clear(); pendingRemoval = null; setVisualizerActive(false); scope.cancel(); MediaController.releaseFuture(future); controller = null }

    private fun itemFor(track: TrackEntity) = MediaItem.Builder().setUri(track.documentUri).setMediaId(track.relativePath)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setExtras(Bundle().apply {
                    putString(QUEUE_ALBUM_ID, track.albumId)
                    putString(QUEUE_OCCURRENCE_ID, UUID.randomUUID().toString())
                    putLong(QUEUE_DURATION_MS, track.durationMs)
                })
                .build(),
        ).build()

    private fun applyResult(
        player: MediaController,
        result: QueueEditResult,
        additions: Map<String, MediaItem> = emptyMap(),
        successNotice: String? = null,
    ): Boolean {
        if (!result.changed) {
            result.notice?.let(::setQueueNotice)
            refresh(player)
            return false
        }
        val existing = List(player.mediaItemCount) { player.getMediaItemAt(it) }.associateBy(::occurrenceId)
        val itemsById = existing + additions
        val start = (player.currentMediaItemIndex + 1).coerceAtLeast(0)
        val upcoming = result.snapshot.entries.drop(start).mapNotNull { itemsById[it.occurrenceId] }
        if (upcoming.size != result.snapshot.entries.size - start) {
            setQueueNotice("QUEUE CHANGED · TRY AGAIN")
            return false
        }
        return runCatching {
            if (player.mediaItemCount == 0) {
                player.setMediaItems(result.snapshot.entries.mapNotNull { itemsById[it.occurrenceId] })
                player.prepare()
                player.playWhenReady = false
            } else {
                player.replaceMediaItems(start, player.mediaItemCount, upcoming)
            }
            player.shuffleModeEnabled = result.snapshot.shuffle
            player.repeatMode = result.snapshot.repeat.toPlayerRepeatMode()
            pendingRemoval = null
            val notice = when {
                result.notice != null && successNotice != null -> "$successNotice · ${result.notice}"
                else -> result.notice ?: successNotice ?: "QUEUE UPDATED"
            }
            setQueueNotice(notice)
            refresh(player)
        }.fold(
            onSuccess = { true },
            onFailure = { setQueueNotice("COULD NOT UPDATE QUEUE"); false },
        )
    }

    private suspend fun canAccessLibrary(): Boolean = withContext(Dispatchers.IO) {
        val source = app.database.libraryDao().librarySource()
        PlaybackAccessPolicy.canPlay(
            hasSource = source != null,
            accessLost = source?.accessLost == true,
            canReadSource = source?.let { app.treeAccess.canRead(it.treeUri) } == true,
        )
    }

    private fun snapshot(player: Player): QueueSnapshot = QueueSnapshot(
        entries = List(player.mediaItemCount) { entryFor(player.getMediaItemAt(it)) },
        currentIndex = player.currentMediaItemIndex,
        shuffle = player.shuffleModeEnabled,
        repeat = player.repeatMode.toRepeatMode(),
    )

    private fun entryFor(item: MediaItem) = QueueEntry(
        occurrenceId = occurrenceId(item),
        relativePath = item.mediaId,
        title = item.mediaMetadata.title?.toString() ?: item.mediaId.substringAfterLast('/'),
        artist = item.mediaMetadata.artist?.toString().orEmpty(),
        album = item.mediaMetadata.albumTitle?.toString().orEmpty(),
        durationMs = item.mediaMetadata.extras?.getLong(QUEUE_DURATION_MS) ?: 0,
    )

    private fun occurrenceId(item: MediaItem): String =
        item.mediaMetadata.extras?.getString(QUEUE_OCCURRENCE_ID) ?: "legacy:${item.mediaId}:${item.hashCode()}"

    private fun indexOfOccurrence(player: Player, targetId: String): Int =
        (0 until player.mediaItemCount).firstOrNull { occurrenceId(player.getMediaItemAt(it)) == targetId } ?: -1

    private fun currentOccurrenceId(player: Player): String? = player.currentMediaItem?.let(::occurrenceId)

    private fun setQueueNotice(message: String) {
        _state.value = _state.value.copy(queueNotice = message, canUndoQueueRemoval = pendingRemoval != null)
    }

    private fun refresh(player: Player) {
        val current = player.currentMediaItem
        val queue = List(player.mediaItemCount) { index ->
            val item = player.getMediaItemAt(index)
            PlaybackQueueItem(
                occurrenceId = occurrenceId(item),
                relativePath = item.mediaId,
                title = item.mediaMetadata.title?.toString() ?: item.mediaId.substringAfterLast('/'),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                album = item.mediaMetadata.albumTitle?.toString().orEmpty(),
                durationMs = item.mediaMetadata.extras?.getLong(QUEUE_DURATION_MS) ?: 0,
                absoluteIndex = index,
            )
        }
        val activeOccurrenceId = current?.let(::occurrenceId)
        val savedProgress = playbackStateRepository.load()?.occurrences
            ?.firstOrNull { it.occurrenceId == activeOccurrenceId }
            ?.accumulatedListeningMs ?: 0
        _state.value = PlaybackUiState(
            title = current?.mediaMetadata?.title?.toString(),
            artist = current?.mediaMetadata?.artist?.toString(),
            album = current?.mediaMetadata?.albumTitle?.toString(),
            albumId = current?.mediaMetadata?.extras?.getString(QUEUE_ALBUM_ID),
            currentPath = current?.mediaId,
            currentOccurrenceId = current?.let(::occurrenceId),
            currentIndex = player.currentMediaItemIndex,
            upNext = queue.drop((player.currentMediaItemIndex + 1).coerceAtLeast(0)),
            positionMs = player.currentPosition,
            durationMs = player.duration.coerceAtLeast(0),
            playing = player.isPlaying,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            error = _state.value.error,
            queueNotice = _state.value.queueNotice,
            canUndoQueueRemoval = pendingRemoval != null,
            completed = player.playbackState == Player.STATE_ENDED,
            meaningfulProgressMs = savedProgress,
        )
    }

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
}

private enum class QueueAddMode { NEXT, APPEND }
private data class PendingQueueAction(val mode: QueueAddMode, val tracks: List<TrackEntity>, val skippedCount: Int)
private data class PendingRemoval(val token: QueueUndoToken, val mediaItem: MediaItem)

data class PlaybackQueueItem(
    val occurrenceId: String,
    val relativePath: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0,
    val absoluteIndex: Int = -1,
) {
    fun toQueueEntry() = QueueEntry(occurrenceId, relativePath, title, artist, album, durationMs)
}

data class PlaybackUiState(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val currentPath: String? = null,
    val currentOccurrenceId: String? = null,
    val currentIndex: Int = -1,
    val upNext: List<PlaybackQueueItem> = emptyList(),
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val playing: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val error: String? = null,
    val queueNotice: String? = null,
    val canUndoQueueRemoval: Boolean = false,
    val completed: Boolean = false,
    val meaningfulProgressMs: Long = 0,
)
