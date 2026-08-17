package ca.stewark.nocturnel.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PlaylistBundleManifestCodecTest {
    @Test fun namesAndOrderingRoundTripThroughJson() {
        val records = listOf(
            PlaylistBundleManifestRecord("Road \"Trip\"\n日本語", "playlists/0001-road-trip.m3u8"),
            PlaylistBundleManifestRecord("Empty", "playlists/0002-empty.m3u8"),
        )
        assertEquals(records, PlaylistBundleManifestCodec.decode(PlaylistBundleManifestCodec.encode(records)).records)
    }

    @Test fun invalidPlaylistDeclarationsAreReportedIndividually() {
        val text = """{"format":"nocturnel-playlists","version":1,"playlists":[
            {"name":"Good","entry":"playlists/good.m3u8"},
            {"name":"Escape","entry":"playlists/../bad.m3u8"},
            {"name":"Absolute","entry":"/playlists/bad.m3u8"},
            {"name":"Backslash","entry":"playlists\\bad.m3u8"},
            {"name":false,"entry":"playlists/type.m3u8"}
        ]}"""
        val decoded = PlaylistBundleManifestCodec.decode(text)
        assertEquals(listOf(PlaylistBundleManifestRecord("Good", "playlists/good.m3u8")), decoded.records)
        assertEquals(4, decoded.invalidRecords)
    }

    @Test fun unsupportedHeadersAndShapesRejectTheBundle() {
        listOf(
            "{}",
            """{"format":"wrong","version":1,"playlists":[]}""",
            """{"format":"nocturnel-playlists","version":2,"playlists":[]}""",
            """{"format":"nocturnel-playlists","version":1,"playlists":{}}""",
        ).forEach { text ->
            try {
                PlaylistBundleManifestCodec.decode(text)
                fail("Expected unsupported manifest: $text")
            } catch (_: UnsupportedPlaylistBundleException) {
            }
        }
    }
}
