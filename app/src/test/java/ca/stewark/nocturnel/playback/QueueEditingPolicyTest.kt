package ca.stewark.nocturnel.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueEditingPolicyTest {
    @Test fun insertNextPreservesBlockOrderAndPutsNewestRequestFirst() {
        val initial = snapshot("history", "current", "later", currentIndex = 1, shuffle = true)
        val first = QueueEditingPolicy.apply(initial, QueueEditCommand.InsertNext(entries("a", "b"))).snapshot
        val second = QueueEditingPolicy.apply(first, QueueEditCommand.InsertNext(entries("c", "d")))

        assertEquals(listOf("history", "current", "c", "d", "a", "b", "later"), second.snapshot.ids())
        assertEquals(1, second.snapshot.currentIndex)
        assertFalse(second.snapshot.shuffle)
    }

    @Test fun appendPreservesDuplicatesAndEmptyQueuesStayUnselected() {
        val duplicateEntries = listOf(entry("one", "same.flac"), entry("two", "same.flac"))
        val appended = QueueEditingPolicy.apply(snapshot("current", "later", currentIndex = 0), QueueEditCommand.Append(duplicateEntries))
        assertEquals(listOf("current", "later", "one", "two"), appended.snapshot.ids())

        val empty = QueueEditingPolicy.apply(QueueSnapshot(), QueueEditCommand.Append(duplicateEntries))
        assertEquals(listOf("one", "two"), empty.snapshot.ids())
        assertEquals(-1, empty.snapshot.currentIndex)
    }

    @Test fun emptyInsertionIsANoOp() {
        val before = snapshot("current", currentIndex = 0, shuffle = true)
        val result = QueueEditingPolicy.apply(before, QueueEditCommand.InsertNext(emptyList()))
        assertEquals(before, result.snapshot)
        assertFalse(result.changed)
    }

    @Test fun moveOnlyChangesUpcomingOccurrencesAndClampsTarget() {
        val before = snapshot("history", "current", "a", "b", "c", currentIndex = 1, shuffle = true)
        val moved = QueueEditingPolicy.apply(before, QueueEditCommand.Move("c", -5))
        assertEquals(listOf("history", "current", "c", "a", "b"), moved.snapshot.ids())
        assertFalse(moved.snapshot.shuffle)

        val rejected = QueueEditingPolicy.apply(before, QueueEditCommand.Move("current", 1))
        assertFalse(rejected.changed)
        assertEquals(before, rejected.snapshot)
    }

    @Test fun removeTargetsOccurrenceRatherThanPathAndReturnsUndoToken() {
        val duplicate = listOf(entry("first", "same.flac"), entry("second", "same.flac"))
        val before = QueueSnapshot(listOf(entry("current")) + duplicate, currentIndex = 0, shuffle = true)
        val result = QueueEditingPolicy.apply(before, QueueEditCommand.Remove("second"))

        assertEquals(listOf("current", "first"), result.snapshot.ids())
        assertEquals("second", result.removed?.entry?.occurrenceId)
        assertEquals(1, result.removed?.upcomingIndex)
        assertFalse(result.snapshot.shuffle)
    }

    @Test fun restoreClampsFormerPositionAfterQueueChanges() {
        val token = QueueUndoToken(entry("restored"), upcomingIndex = 8)
        val result = QueueEditingPolicy.apply(snapshot("current", "next", currentIndex = 0), QueueEditCommand.RestoreRemoved(token))
        assertEquals(listOf("current", "next", "restored"), result.snapshot.ids())
        assertNull(result.removed)
    }

    @Test fun clearRetainsHistoryAndCurrentAndNormalizesModes() {
        val repeatAll = QueueSnapshot(entries("history", "current", "a"), 1, shuffle = true, repeat = RepeatMode.ALL)
        val cleared = QueueEditingPolicy.apply(repeatAll, QueueEditCommand.ClearUpcoming)
        assertEquals(listOf("history", "current"), cleared.snapshot.ids())
        assertFalse(cleared.snapshot.shuffle)
        assertEquals(RepeatMode.OFF, cleared.snapshot.repeat)

        val repeatOne = QueueSnapshot(entries("current", "a"), 0, repeat = RepeatMode.ONE)
        assertEquals(RepeatMode.ONE, QueueEditingPolicy.apply(repeatOne, QueueEditCommand.ClearUpcoming).snapshot.repeat)
    }

    @Test fun invalidOrNoOpCommandsDoNotMutateState() {
        val before = snapshot("current", "next", currentIndex = 0)
        val unknown = QueueEditingPolicy.apply(before, QueueEditCommand.Remove("missing"))
        val samePlace = QueueEditingPolicy.apply(before, QueueEditCommand.Move("next", 0))
        assertFalse(unknown.changed)
        assertNotNull(unknown.notice)
        assertFalse(samePlace.changed)
        assertEquals(before, samePlace.snapshot)
    }

    private fun snapshot(vararg ids: String, currentIndex: Int, shuffle: Boolean = false) =
        QueueSnapshot(entries(*ids), currentIndex, shuffle)

    private fun entries(vararg ids: String) = ids.map(::entry)
    private fun entry(id: String, path: String = "$id.flac") = QueueEntry(id, path, id, "Artist", "Album", 1_000)
    private fun QueueSnapshot.ids() = entries.map { it.occurrenceId }
}
