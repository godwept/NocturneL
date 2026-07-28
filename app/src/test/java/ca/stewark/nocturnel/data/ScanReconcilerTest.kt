package ca.stewark.nocturnel.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanReconcilerTest {
    @Test
    fun findsPreviouslyKnownUnseenTracks() {
        assertEquals(setOf("Album/02.mp3"), ScanReconciler.missingPaths(setOf("Album/01.mp3", "Album/02.mp3"), setOf("Album/01.mp3")))
    }

    @Test
    fun countsAddedChangedAndMissingTracks() {
        val previous = listOf(
            TrackFingerprint("Album/01.mp3", "old"),
            TrackFingerprint("Album/02.mp3", "same"),
            TrackFingerprint("Album/03.mp3", "gone"),
        )
        val current = listOf(
            TrackFingerprint("Album/01.mp3", "new"),
            TrackFingerprint("Album/02.mp3", "same"),
            TrackFingerprint("Album/04.mp3", "added"),
        )

        assertEquals(ReconciliationCounts(added = 1, changed = 1, missing = 1), ScanReconciler.count(previous, current))
    }
}
