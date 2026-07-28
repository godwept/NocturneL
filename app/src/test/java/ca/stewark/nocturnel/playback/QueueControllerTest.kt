package ca.stewark.nocturnel.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueControllerTest {
    @Test
    fun repeatAllWrapsAtEndOfQueue() {
        val next = QueueController(QueueState(listOf("a", "b"), 1, repeat = RepeatMode.ALL)).next { true }
        assertEquals(0, next.currentIndex)
    }

    @Test
    fun nextSkipsUnavailableTracks() {
        val next = QueueController(QueueState(listOf("a", "b"), 0)).next { it == "b" }
        assertEquals(1, next.currentIndex)
    }

    @Test
    fun repeatOneKeepsCurrentTrack() {
        val state = QueueState(listOf("a", "b"), 0, repeat = RepeatMode.ONE)

        assertEquals(state, QueueController(state).next { true })
    }

    @Test
    fun shuffleKeepsCurrentTrackUntilAdvance() {
        val state = QueueState(listOf("a", "b", "c"), currentIndex = 1)
        val shuffled = QueueController(state).withShuffle(true) { listOf(2, 0) }

        assertEquals(1, shuffled.currentIndex)
        assertEquals(listOf(1, 2, 0), shuffled.playOrder)
        assertEquals(2, QueueController(shuffled).next { true }.currentIndex)
    }

    @Test
    fun seekRejectsUnavailableTarget() {
        val state = QueueState(listOf("a", "b"), currentIndex = 0)

        assertSame(state, QueueController(state).seekTo(1) { false })
    }

    @Test
    fun playbackRequiresAnAccessibleSelectedSource() {
        assertTrue(PlaybackAccessPolicy.canPlay(hasSource = true, accessLost = false, canReadSource = true))
        assertFalse(PlaybackAccessPolicy.canPlay(hasSource = true, accessLost = true, canReadSource = true))
        assertFalse(PlaybackAccessPolicy.canPlay(hasSource = true, accessLost = false, canReadSource = false))
        assertFalse(PlaybackAccessPolicy.canPlay(hasSource = false, accessLost = false, canReadSource = false))
    }
}
