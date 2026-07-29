package ca.stewark.nocturnel.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class UiFixturesTest {
    @Test fun fixturesAreRelated() {
        assertEquals(sampleAlbum.id, sampleTracks.first().albumId)
        assertEquals(1L, samplePlaylist.id)
    }
}
