package ca.stewark.nocturnel.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueShufflePolicyTest {
    @Test fun enablingShuffleRandomizesEveryUpcomingOccurrenceExactlyOnce() {
        val snapshot = QueueSnapshot(
            entries = entries("history", "current", "a", "b", "c", "d"),
            currentIndex = 1,
        )

        val shuffled = QueueShufflePolicy.toggle(snapshot) { it.reversed() }

        assertEquals(listOf("history", "current", "d", "c", "b", "a"), shuffled.entries.map { it.occurrenceId })
        assertEquals(1, shuffled.currentIndex)
        assertTrue(shuffled.shuffle)
    }

    @Test fun disablingShuffleKeepsTheVisibleQueueOrder() {
        val snapshot = QueueSnapshot(entries("current", "c", "a", "b"), currentIndex = 0, shuffle = true)

        val unshuffled = QueueShufflePolicy.toggle(snapshot)

        assertEquals(snapshot.entries, unshuffled.entries)
        assertFalse(unshuffled.shuffle)
    }

    @Test fun invalidShufflerCannotDropOrDuplicateQueueOccurrences() {
        val snapshot = QueueSnapshot(entries("current", "a", "b"), currentIndex = 0)

        val shuffled = QueueShufflePolicy.toggle(snapshot) { listOf(it.first(), it.first()) }

        assertEquals(snapshot.entries, shuffled.entries)
        assertTrue(shuffled.shuffle)
    }

    private fun entries(vararg ids: String) = ids.map { id ->
        QueueEntry(id, "$id.flac", id, "Artist", "Album", 1_000)
    }
}
