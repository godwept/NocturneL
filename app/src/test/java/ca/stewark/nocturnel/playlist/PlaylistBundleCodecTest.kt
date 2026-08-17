package ca.stewark.nocturnel.playlist

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlaylistBundleCodecTest {
    @Test fun contractsAndEntryNamesAreStable() {
        assertEquals("nocturnel-playlists", PLAYLIST_BUNDLE_FORMAT)
        assertEquals(1, PLAYLIST_BUNDLE_VERSION)
        assertEquals("nocturnel-playlists.json", PLAYLIST_BUNDLE_MANIFEST)
        assertEquals(1_000, MAX_BUNDLE_PLAYLISTS)
        assertEquals(4 * 1024 * 1024, MAX_BUNDLE_PLAYLIST_BYTES)
        assertEquals(64L * 1024 * 1024, MAX_BUNDLE_UNCOMPRESSED_BYTES)
        assertEquals("playlists/0001-road-trip.m3u8", bundleEntryName(0, "Road Trip"))
        assertEquals("playlists/0002-untitled-playlist.m3u8", bundleEntryName(1, "<>:\"/\\|?*"))
    }

    @Test fun roundTripsNamesEmptyPlaylistsOrderAndMissingPaths() {
        val original = listOf(
            PlaylistBundlePlaylist("Road \"Trip\"", listOf("A/02.flac", "A/01.flac", "missing.flac")),
            PlaylistBundlePlaylist("Road \"Trip\"", emptyList()),
            PlaylistBundlePlaylist("日本語", listOf("B/01.mp3")),
        )
        val bytes = ByteArrayOutputStream().also { PlaylistBundleCodec.encode(original, it) }.toByteArray()
        val result = PlaylistBundleCodec.decode(ByteArrayInputStream(bytes))
        assertEquals(original, result.playlists)
        assertEquals(0, result.skippedPlaylists)
        assertEquals(0, result.skippedTracks)
    }

    @Test fun skipsMissingPlaylistAndUnsafeTrackButImportsValidData() {
        val manifest = """{"format":"nocturnel-playlists","version":1,"playlists":[{"name":"Good","entry":"playlists/good.m3u8"},{"name":"Missing","entry":"playlists/missing.m3u8"}]}"""
        val bytes = zip(mapOf(PLAYLIST_BUNDLE_MANIFEST to manifest, "playlists/good.m3u8" to "#EXTM3U\ngood.flac\n../bad.flac\n"))
        val result = PlaylistBundleCodec.decode(ByteArrayInputStream(bytes))
        assertEquals(listOf(PlaylistBundlePlaylist("Good", listOf("good.flac"))), result.playlists)
        assertEquals(1, result.skippedPlaylists)
        assertEquals(1, result.skippedTracks)
    }

    @Test fun rejectsMissingManifest() {
        expectFailure { PlaylistBundleCodec.decode(ByteArrayInputStream(zip(mapOf("playlists/a.m3u8" to "#EXTM3U\n")))) }
    }

    @Test fun limitsActualUncompressedBytes() {
        val original = listOf(PlaylistBundlePlaylist("Large", listOf("a.flac")))
        val bytes = ByteArrayOutputStream().also { PlaylistBundleCodec.encode(original, it) }.toByteArray()
        expectFailure {
            PlaylistBundleCodec.decode(ByteArrayInputStream(bytes), PlaylistBundleLimits(10, 8, 16))
        }
    }

    @Test fun oversizedPlaylistIsSkippedWhileAnotherImports() {
        val manifest = """{"format":"nocturnel-playlists","version":1,"playlists":[{"name":"Large","entry":"playlists/large.m3u8"},{"name":"Good","entry":"playlists/good.m3u8"}]}"""
        val bytes = zip(mapOf(
            PLAYLIST_BUNDLE_MANIFEST to manifest,
            "playlists/large.m3u8" to "#EXTM3U\nthis-is-too-large.flac\n",
            "playlists/good.m3u8" to "a.flac",
        ))
        val result = PlaylistBundleCodec.decode(ByteArrayInputStream(bytes), PlaylistBundleLimits(10, 12, 10_000))
        assertEquals(listOf(PlaylistBundlePlaylist("Good", listOf("a.flac"))), result.playlists)
        assertEquals(1, result.skippedPlaylists)
    }

    @Test fun duplicateManifestMappingSkipsBothRecords() {
        val manifest = """{"format":"nocturnel-playlists","version":1,"playlists":[{"name":"One","entry":"playlists/shared.m3u8"},{"name":"Two","entry":"playlists/shared.m3u8"}]}"""
        val result = PlaylistBundleCodec.decode(ByteArrayInputStream(zip(mapOf(
            PLAYLIST_BUNDLE_MANIFEST to manifest,
            "playlists/shared.m3u8" to "a.flac",
        ))))
        assertEquals(emptyList<PlaylistBundlePlaylist>(), result.playlists)
        assertEquals(2, result.skippedPlaylists)
    }

    private fun zip(entries: Map<String, String>): ByteArray = ByteArrayOutputStream().also { output ->
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, value) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }.toByteArray()

    private fun expectFailure(block: () -> Unit) {
        try {
            block()
            fail("Expected failure")
        } catch (_: UnsupportedPlaylistBundleException) {
        }
    }
}
