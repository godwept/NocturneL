package ca.stewark.nocturnel.playback

object PlaybackRestorePolicy {
    fun shouldAutoPlay(snapshot: PlaybackSnapshot, currentSessionId: String): Boolean =
        snapshot.wasPlaying && snapshot.playbackSessionId != null && snapshot.playbackSessionId == currentSessionId
}
