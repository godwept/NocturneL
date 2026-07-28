package ca.stewark.nocturnel.playback

import org.junit.Assert.assertEquals
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
}
