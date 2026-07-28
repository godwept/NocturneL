package ca.stewark.nocturnel.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioFocusHandlerTest {
    @Test
    fun transientLossPausesAndGainRestoresOnlyWhenPlaybackWasActive() {
        val handler = AudioFocusHandler()

        assertEquals(AudioFocusAction.PAUSE, handler.onTransientLoss(isPlaying = true))
        assertEquals(AudioFocusAction.RESUME, handler.onGain())
        assertEquals(AudioFocusAction.NONE, handler.onGain())
    }

    @Test
    fun userPausePreventsResumeAfterTransientLoss() {
        val handler = AudioFocusHandler()

        handler.onTransientLoss(isPlaying = true)
        handler.onUserPause()

        assertEquals(AudioFocusAction.NONE, handler.onGain())
    }

    @Test
    fun duckingDoesNotScheduleAResume() {
        val handler = AudioFocusHandler()

        assertEquals(AudioFocusAction.DUCK, handler.onDuck())
        assertEquals(AudioFocusAction.RESTORE_VOLUME, handler.onGain())
    }
}
