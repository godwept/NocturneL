package ca.stewark.nocturnel.ui.listening

import ca.stewark.nocturnel.playback.PlaybackUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class ResumeProjectionTest {
    private val paused = PlaybackUiState(currentPath = "one.flac", title = "One", positionMs = 10_000, meaningfulProgressMs = 10_000, durationMs = 100_000)

    @Test fun requiresMeaningfulPausedIncompleteProgress() {
        assertNull(resumeState(paused.copy(meaningfulProgressMs = 9_999), true))
        assertNull(resumeState(paused.copy(playing = true), true))
        assertNull(resumeState(paused.copy(completed = true), true))
        assertNotNull(resumeState(paused, true))
    }

    @Test fun accessLossKeepsResumeVisibleButDisabled() {
        assertFalse(resumeState(paused, false)!!.enabled)
    }
}
