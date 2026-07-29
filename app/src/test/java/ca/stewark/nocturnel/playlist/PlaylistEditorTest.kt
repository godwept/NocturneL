package ca.stewark.nocturnel.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistEditorTest {
    @Test
    fun appendsOnlyMissingTracksInAlbumOrder() {
        val result = PlaylistEditor.appendDistinct(
            existing = listOf("old.flac", "02.flac"),
            candidates = listOf("01.flac", "02.flac", "03.flac"),
        )

        assertEquals(listOf("old.flac", "02.flac", "01.flac", "03.flac"), result.paths)
        assertEquals(2, result.added)
        assertEquals(1, result.skipped)
    }

    @Test
    fun bulkAppendReportsCompleteOverlapAndEmptyInput() {
        assertEquals(
            AppendDistinctResult(listOf("01.flac"), added = 0, skipped = 1),
            PlaylistEditor.appendDistinct(listOf("01.flac"), listOf("01.flac")),
        )
        assertEquals(
            AppendDistinctResult(listOf("01.flac"), added = 0, skipped = 0),
            PlaylistEditor.appendDistinct(listOf("01.flac"), emptyList()),
        )
    }

    @Test
    fun bulkAppendSkipsRepeatedCandidatePaths() {
        val result = PlaylistEditor.appendDistinct(emptyList(), listOf("01.flac", "01.flac", "02.flac"))

        assertEquals(listOf("01.flac", "02.flac"), result.paths)
        assertEquals(2, result.added)
        assertEquals(1, result.skipped)
    }

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
