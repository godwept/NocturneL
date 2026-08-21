package ca.stewark.nocturnel.ui.listening

import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySortModeTest {
    @Test fun `artist is the default and the modes cycle in display order`() {
        assertEquals(LibrarySortMode.ARTIST, LibrarySortMode.DEFAULT)
        assertEquals(LibrarySortMode.TITLE, LibrarySortMode.ARTIST.next())
        assertEquals(LibrarySortMode.YEAR, LibrarySortMode.TITLE.next())
        assertEquals(LibrarySortMode.MOST_PLAYED, LibrarySortMode.YEAR.next())
        assertEquals(LibrarySortMode.ARTIST, LibrarySortMode.MOST_PLAYED.next())
    }

    @Test fun `persisted values restore or fall back to artist`() {
        assertEquals(LibrarySortMode.ARTIST, LibrarySortMode.fromPersisted(null))
        assertEquals(LibrarySortMode.ARTIST, LibrarySortMode.fromPersisted("UNKNOWN"))
        assertEquals(LibrarySortMode.MOST_PLAYED, LibrarySortMode.fromPersisted("MOST_PLAYED"))
    }
}
