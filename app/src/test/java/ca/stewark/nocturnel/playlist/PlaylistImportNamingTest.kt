package ca.stewark.nocturnel.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistImportNamingTest {
    @Test fun choosesAndReservesNextCaseInsensitiveSuffix() {
        val used = mutableSetOf("Favorites", "favorites (2)")
        assertEquals("Favorites (3)", uniqueImportedPlaylistName("Favorites", used))
        assertEquals("Road Trip", uniqueImportedPlaylistName(" Road Trip ", used))
        assertEquals("road trip (2)", uniqueImportedPlaylistName("road trip", used))
    }
}
