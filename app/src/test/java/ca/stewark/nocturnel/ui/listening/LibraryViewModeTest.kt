package ca.stewark.nocturnel.ui.listening

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryViewModeTest {
    @Test fun defaultsCyclesAndParsesPersistedValues() {
        assertEquals(LibraryViewMode.GRID, LibraryViewMode.DEFAULT)
        assertEquals(LibraryViewMode.FLOW, LibraryViewMode.GRID.next())
        assertEquals(LibraryViewMode.GRID, LibraryViewMode.FLOW.next())
        assertEquals(LibraryViewMode.GRID, LibraryViewMode.fromPersisted("GRID"))
        assertEquals(LibraryViewMode.FLOW, LibraryViewMode.fromPersisted("FLOW"))
        assertEquals(LibraryViewMode.GRID, LibraryViewMode.fromPersisted(null))
        assertEquals(LibraryViewMode.GRID, LibraryViewMode.fromPersisted("UNKNOWN"))
    }
}
