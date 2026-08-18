package ca.stewark.nocturnel.data.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningEntitiesTest {
    @Test fun historyRetainsStableQualificationIdentity() {
        val event = PlayHistoryEntity(qualificationId = "occurrence-1", relativePath = "album/one.flac", playedAtEpochMillis = 42)
        assertEquals(0, event.id)
        assertEquals("occurrence-1", event.qualificationId)
        assertEquals("album/one.flac", event.relativePath)
        assertEquals(42, event.playedAtEpochMillis)
    }
}
