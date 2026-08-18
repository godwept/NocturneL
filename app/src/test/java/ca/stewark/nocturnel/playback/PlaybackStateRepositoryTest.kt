package ca.stewark.nocturnel.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackStateRepositoryTest {
    @Test
    fun savedStateRoundTripsQueuePositionAndRepeatMode() {
        val snapshot = PlaybackSnapshot(
            paths = listOf("Artist/Album/01 - One.mp3", "odd % name/02.mp3"),
            currentIndex = 1,
            positionMs = 42_000,
            shuffle = true,
            repeat = RepeatMode.ALL,
            wasPlaying = true,
        )

        assertEquals(snapshot, PlaybackStateCodec.decode(PlaybackStateCodec.encode(snapshot)))
    }

    @Test
    fun invalidSavedStateIsIgnored() {
        assertNull(PlaybackStateCodec.decode("not-a-playback-state"))
    }

    @Test
    fun restoreSkipsUnavailableTracksAndKeepsCurrentPositionWhenCurrentTrackExists() {
        val snapshot = PlaybackSnapshot(
            paths = listOf("missing.mp3", "current.mp3", "next.mp3"),
            currentIndex = 1,
            positionMs = 7_500,
            shuffle = false,
            repeat = RepeatMode.OFF,
            wasPlaying = false,
        )

        assertEquals(
            PlaybackRestorePlan(listOf("current.mp3", "next.mp3"), currentIndex = 0, positionMs = 7_500),
            PlaybackRestorePlanner.plan(snapshot, setOf("current.mp3", "next.mp3")),
        )
    }

    @Test
    fun restoreStartsAtBeginningWhenSavedCurrentTrackIsUnavailable() {
        val snapshot = PlaybackSnapshot(
            paths = listOf("missing.mp3", "next.mp3"),
            currentIndex = 0,
            positionMs = 7_500,
            shuffle = false,
            repeat = RepeatMode.OFF,
            wasPlaying = false,
        )

        assertEquals(
            PlaybackRestorePlan(listOf("next.mp3"), currentIndex = 0, positionMs = 0),
            PlaybackRestorePlanner.plan(snapshot, setOf("next.mp3")),
        )
    }

    @Test
    fun restorePreservesTheSelectedDuplicateOccurrence() {
        val snapshot = PlaybackSnapshot(
            paths = listOf("same.flac", "other.flac", "same.flac"),
            currentIndex = 2,
            positionMs = 9_000,
            shuffle = false,
            repeat = RepeatMode.OFF,
            wasPlaying = false,
        )

        assertEquals(
            PlaybackRestorePlan(snapshot.paths, currentIndex = 2, positionMs = 9_000),
            PlaybackRestorePlanner.plan(snapshot, setOf("same.flac", "other.flac")),
        )
    }

    @Test
    fun restoreRemapsDuplicateOccurrenceAfterMissingEntriesAreRemoved() {
        val snapshot = PlaybackSnapshot(
            paths = listOf("missing.flac", "same.flac", "same.flac"),
            currentIndex = 2,
            positionMs = 3_000,
            shuffle = false,
            repeat = RepeatMode.OFF,
            wasPlaying = false,
        )

        assertEquals(
            PlaybackRestorePlan(listOf("same.flac", "same.flac"), currentIndex = 1, positionMs = 3_000),
            PlaybackRestorePlanner.plan(snapshot, setOf("same.flac")),
        )
    }
}
