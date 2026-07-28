package ca.stewark.nocturnel.playback

enum class RepeatMode { OFF, ONE, ALL }

data class QueueState(
    val paths: List<String> = emptyList(),
    val currentIndex: Int = -1,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
)
