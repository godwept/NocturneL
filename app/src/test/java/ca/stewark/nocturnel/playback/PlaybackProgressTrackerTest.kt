package ca.stewark.nocturnel.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackProgressTrackerTest {
    @Test fun qualifiesAtHalfOrFourMinutesWhicheverComesFirst() {
        val short = PlaybackProgressTracker()
        short.update("one", "one.flac", 100_000, true, 0)
        assertNull(short.update("one", "one.flac", 100_000, true, 49_999))
        assertEquals("one", short.update("one", "one.flac", 100_000, true, 50_000)?.qualificationId)

        val long = PlaybackProgressTracker()
        long.update("two", "two.flac", 600_000, true, 0)
        assertEquals("two", long.update("two", "two.flac", 600_000, true, 240_000)?.qualificationId)
    }

    @Test fun pauseAndSeekDoNotAddListeningTime() {
        val tracker = PlaybackProgressTracker()
        tracker.update("one", "one.flac", 100_000, true, 0)
        tracker.update("one", "one.flac", 100_000, false, 20_000)
        tracker.update("one", "one.flac", 100_000, false, 80_000)
        tracker.update("one", "one.flac", 100_000, true, 80_000)
        tracker.discontinuity(90_000)
        assertNull(tracker.update("one", "one.flac", 100_000, true, 109_999))
        assertEquals("one", tracker.update("one", "one.flac", 100_000, true, 110_000)?.qualificationId)
    }

    @Test fun unknownDurationQualifiesOnNaturalCompletion() {
        val tracker = PlaybackProgressTracker()
        tracker.update("one", "one.flac", 0, true, 0)
        assertEquals("one", tracker.complete(1_000)?.qualificationId)
        assertNull(tracker.complete(2_000))
    }
}
