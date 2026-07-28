package ca.stewark.nocturnel.library.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryModelsTest {
    @Test
    fun trackStatusesDistinguishPlayableMissingAndUnsupported() {
        assertEquals(4, TrackStatus.entries.size)
    }
}
