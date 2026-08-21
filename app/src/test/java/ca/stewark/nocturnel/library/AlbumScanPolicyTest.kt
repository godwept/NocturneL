package ca.stewark.nocturnel.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumScanPolicyTest {
    @Test fun albumIsCleanOnlyWhenMembershipAndEveryTrackAreReusable() {
        val current = setOf("01.mp3", "02.mp3")
        assertTrue(AlbumScanPolicy.isClean(current, current, current))
        assertFalse(AlbumScanPolicy.isClean(current + "03.mp3", current, current))
        assertFalse(AlbumScanPolicy.isClean(current, current + "gone.mp3", current))
        assertFalse(AlbumScanPolicy.isClean(current, current, setOf("01.mp3")))
    }
}
