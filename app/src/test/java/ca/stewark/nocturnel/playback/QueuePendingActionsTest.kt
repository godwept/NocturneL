package ca.stewark.nocturnel.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueuePendingActionsTest {
    @Test fun drainsInCallOrderExactlyOnce() {
        val pending = PendingQueueActions<String>()
        pending.add("next")
        pending.add("append")
        assertEquals(listOf("next", "append"), pending.drain())
        assertTrue(pending.drain().isEmpty())
    }

    @Test fun clearDropsPendingActions() {
        val pending = PendingQueueActions<String>()
        pending.add("next")
        pending.clear()
        assertTrue(pending.drain().isEmpty())
    }
}
