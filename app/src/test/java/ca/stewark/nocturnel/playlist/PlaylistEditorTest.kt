package ca.stewark.nocturnel.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistEditorTest {
    @Test
    fun addsTracksInRequestedOrderAndKeepsMissingReferences() {
        val edited = PlaylistEditor.add(listOf("Music/present.mp3", "Music/missing.mp3"), "Music/new.mp3")

        assertEquals(listOf("Music/present.mp3", "Music/missing.mp3", "Music/new.mp3"), edited)
    }

    @Test
    fun removesOnlyTheSelectedPlaylistEntry() {
        val edited = PlaylistEditor.removeAt(listOf("same.mp3", "same.mp3", "other.mp3"), 1)

        assertEquals(listOf("same.mp3", "other.mp3"), edited)
    }

    @Test
    fun movesAnEntryAndPreservesAllReferences() {
        val edited = PlaylistEditor.move(listOf("a", "b", "c"), fromIndex = 2, toIndex = 0)

        assertEquals(listOf("c", "a", "b"), edited)
    }
}
