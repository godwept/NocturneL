package ca.stewark.nocturnel.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRestorePolicyTest {
    @Test fun onlySameApplicationSessionMayAutoPlay() {
        val base = PlaybackSnapshot(listOf("one"), 0, 10, false, RepeatMode.OFF, wasPlaying = true)
        assertFalse(PlaybackRestorePolicy.shouldAutoPlay(base, "new"))
        assertFalse(PlaybackRestorePolicy.shouldAutoPlay(base.copy(playbackSessionId = "old"), "new"))
        assertTrue(PlaybackRestorePolicy.shouldAutoPlay(base.copy(playbackSessionId = "same"), "same"))
    }
}
