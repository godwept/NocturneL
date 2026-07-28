package ca.stewark.nocturnel.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanReconcilerTest {
    @Test
    fun findsPreviouslyKnownUnseenTracks() {
        assertEquals(setOf("Album/02.mp3"), ScanReconciler.missingPaths(setOf("Album/01.mp3", "Album/02.mp3"), setOf("Album/01.mp3")))
    }
}
