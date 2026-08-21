package ca.stewark.nocturnel.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackFingerprintFieldsTest {
    @Test fun legacyConstructionDefaultsFingerprintToUnknown() {
        val track = track()
        assertNull(track.fileSizeBytes)
        assertNull(track.lastModifiedEpochMillis)
    }

    @Test fun discoveredFingerprintIsRetained() {
        val track = track(fileSizeBytes = 123, lastModifiedEpochMillis = 456)
        assertEquals(123L, track.fileSizeBytes)
        assertEquals(456L, track.lastModifiedEpochMillis)
    }

    private fun track(
        fileSizeBytes: Long? = null,
        lastModifiedEpochMillis: Long? = null,
    ) = TrackEntity(
        relativePath = "Album/01.mp3",
        documentUri = "content://track/1",
        albumId = "album",
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 1_000,
        trackNumber = 1,
        discNumber = 1,
        status = "PLAYABLE",
        lastSeenScanEpochMillis = 1,
        fileSizeBytes = fileSizeBytes,
        lastModifiedEpochMillis = lastModifiedEpochMillis,
    )
}
