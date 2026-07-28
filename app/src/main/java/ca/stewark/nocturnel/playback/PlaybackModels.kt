package ca.stewark.nocturnel.playback

enum class RepeatMode { OFF, ONE, ALL }

object PlaybackAccessPolicy {
    fun canPlay(hasSource: Boolean, accessLost: Boolean, canReadSource: Boolean): Boolean =
        hasSource && !accessLost && canReadSource
}

data class QueueState(
    val paths: List<String> = emptyList(),
    val currentIndex: Int = -1,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val playOrder: List<Int> = emptyList(),
)
