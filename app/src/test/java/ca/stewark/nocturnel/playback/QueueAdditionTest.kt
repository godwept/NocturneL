package ca.stewark.nocturnel.playback

import ca.stewark.nocturnel.data.entity.TrackEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueAdditionTest {
    @Test fun keepsPlayableOrderAndReportsSkippedTracks() {
        val result = queueAddition(listOf(track("a"), track("b", "MISSING"), track("a")), externallySkipped = 2)
        assertEquals(listOf("a", "a"), result.tracks.map { it.relativePath })
        assertEquals(3, result.skipped)
        assertEquals("QUEUED 2 TRACK(S) · SKIPPED 3", result.message)
    }

    @Test fun reportsWhenNothingCanBeQueued() {
        assertEquals("NO PLAYABLE TRACKS", queueAddition(listOf(track("a", "MISSING"))).message)
    }

    private fun track(path: String, status: String = "PLAYABLE") =
        TrackEntity(path, "content://$path", "album", path, "Artist", "Album", 1_000, 1, 1, status, 1)
}
