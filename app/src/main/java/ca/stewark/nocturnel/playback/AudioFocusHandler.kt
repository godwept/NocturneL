package ca.stewark.nocturnel.playback

enum class AudioFocusAction { NONE, PAUSE, RESUME, DUCK, RESTORE_VOLUME }

/**
 * Testable interruption policy. Media3 owns the platform AudioManager integration; this
 * policy records whether a transient interruption may resume without overriding a user pause.
 */
class AudioFocusHandler {
    private var resumeAfterGain = false
    private var ducked = false

    fun onTransientLoss(isPlaying: Boolean): AudioFocusAction {
        resumeAfterGain = isPlaying
        return if (isPlaying) AudioFocusAction.PAUSE else AudioFocusAction.NONE
    }

    fun onPermanentLoss(isPlaying: Boolean): AudioFocusAction {
        resumeAfterGain = false
        return if (isPlaying) AudioFocusAction.PAUSE else AudioFocusAction.NONE
    }

    fun onDuck(): AudioFocusAction {
        ducked = true
        return AudioFocusAction.DUCK
    }

    fun onUserPause() {
        resumeAfterGain = false
    }

    fun onGain(): AudioFocusAction = when {
        ducked -> {
            ducked = false
            AudioFocusAction.RESTORE_VOLUME
        }
        resumeAfterGain -> {
            resumeAfterGain = false
            AudioFocusAction.RESUME
        }
        else -> AudioFocusAction.NONE
    }
}
