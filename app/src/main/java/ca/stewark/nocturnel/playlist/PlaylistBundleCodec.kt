package ca.stewark.nocturnel.playlist

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal fun bundleEntryName(index: Int, name: String): String {
    val slug = name.lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-', '.', ' ')
        .take(80)
        .trimEnd('-')
        .ifBlank { "untitled-playlist" }
    return "playlists/${(index + 1).toString().padStart(4, '0')}-$slug.m3u8"
}

object PlaylistBundleCodec {
    fun encode(playlists: List<PlaylistBundlePlaylist>, output: OutputStream) {
        require(playlists.size <= MAX_BUNDLE_PLAYLISTS)
        val records = playlists.mapIndexed { index, playlist -> PlaylistBundleManifestRecord(playlist.name, bundleEntryName(index, playlist.name)) }
        val zip = ZipOutputStream(output)
        writeEntry(zip, PLAYLIST_BUNDLE_MANIFEST, PlaylistBundleManifestCodec.encode(records).toByteArray(Charsets.UTF_8))
        playlists.zip(records).forEach { (playlist, record) ->
            writeEntry(zip, record.entry, M3u8Codec.encode(playlist.paths).toByteArray(Charsets.UTF_8))
        }
        zip.finish()
        zip.flush()
    }

    fun decode(input: InputStream, limits: PlaylistBundleLimits = PlaylistBundleLimits()): PlaylistBundleDecodeResult {
        val entries = linkedMapOf<String, ByteArray?>()
        val duplicates = mutableSetOf<String>()
        var total = 0L
        try {
            val zip = ZipInputStream(input)
            while (true) {
                val entry = zip.nextEntry ?: break
                val bytes = ByteArrayOutputStream()
                var oversized = entry.isDirectory
                val buffer = ByteArray(8_192)
                while (true) {
                    val read = zip.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > limits.maxTotalBytes) throw UnsupportedPlaylistBundleException("Playlist bundle is too large")
                    if (entry.name.startsWith("playlists/") && bytes.size() + read > limits.maxPlaylistBytes) oversized = true
                    if (!oversized) bytes.write(buffer, 0, read)
                }
                if (entries.containsKey(entry.name)) duplicates += entry.name
                entries[entry.name] = if (oversized) null else bytes.toByteArray()
                zip.closeEntry()
            }
        } catch (error: UnsupportedPlaylistBundleException) {
            throw error
        } catch (error: java.io.IOException) {
            throw UnsupportedPlaylistBundleException("Invalid playlist bundle", error)
        }
        val manifestBytes = entries[PLAYLIST_BUNDLE_MANIFEST]
            ?: throw UnsupportedPlaylistBundleException("Playlist bundle manifest is missing")
        if (PLAYLIST_BUNDLE_MANIFEST in duplicates) throw UnsupportedPlaylistBundleException("Playlist bundle manifest is duplicated")
        val manifest = PlaylistBundleManifestCodec.decode(decodeUtf8(manifestBytes), limits.maxPlaylists)
        val mappingCounts = manifest.records.groupingBy { it.entry }.eachCount()
        var skippedPlaylists = manifest.invalidRecords
        var skippedTracks = 0
        val playlists = manifest.records.mapNotNull { record ->
            val bytes = entries[record.entry]
            if (bytes == null || record.entry in duplicates || mappingCounts[record.entry] != 1) {
                skippedPlaylists++
                null
            } else {
                val text = runCatching { decodeUtf8(bytes) }.getOrNull()
                if (text == null) {
                    skippedPlaylists++
                    null
                } else {
                    val parsed = M3u8Codec.parsePortable(text)
                    skippedTracks += parsed.skipped.size
                    PlaylistBundlePlaylist(record.name, parsed.paths)
                }
            }
        }
        return PlaylistBundleDecodeResult(playlists, skippedPlaylists, skippedTracks)
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun decodeUtf8(bytes: ByteArray): String = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(bytes)).toString()
}
