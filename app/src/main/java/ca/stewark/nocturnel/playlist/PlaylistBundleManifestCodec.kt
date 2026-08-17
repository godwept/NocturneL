package ca.stewark.nocturnel.playlist

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

internal data class PlaylistBundleManifestRecord(val name: String, val entry: String)
internal data class PlaylistBundleManifest(val records: List<PlaylistBundleManifestRecord>, val invalidRecords: Int)

internal object PlaylistBundleManifestCodec {
    fun encode(records: List<PlaylistBundleManifestRecord>): String = buildJsonObject {
        put("format", PLAYLIST_BUNDLE_FORMAT)
        put("version", PLAYLIST_BUNDLE_VERSION)
        put("playlists", buildJsonArray {
            records.forEach { record ->
                add(buildJsonObject { put("name", record.name); put("entry", record.entry) })
            }
        })
    }.toString()

    fun decode(text: String, maxPlaylists: Int = MAX_BUNDLE_PLAYLISTS): PlaylistBundleManifest {
        val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            ?: throw UnsupportedPlaylistBundleException("Invalid playlist bundle manifest")
        if ((root["format"] as? JsonPrimitive)?.content != PLAYLIST_BUNDLE_FORMAT ||
            (root["version"] as? JsonPrimitive)?.intOrNull != PLAYLIST_BUNDLE_VERSION
        ) throw UnsupportedPlaylistBundleException("Unsupported playlist bundle")
        val entries = root["playlists"] as? JsonArray
            ?: throw UnsupportedPlaylistBundleException("Invalid playlist bundle playlists")
        if (entries.size > maxPlaylists) throw UnsupportedPlaylistBundleException("Playlist bundle contains too many playlists")
        var invalid = 0
        val records = entries.mapNotNull { element ->
            val item = element as? JsonObject
            val nameValue = item?.get("name") as? JsonPrimitive
            val entryValue = item?.get("entry") as? JsonPrimitive
            val name = nameValue?.takeIf { it.isString }?.content
            val entry = entryValue?.takeIf { it.isString }?.content
            if (name == null || entry == null || !isSafePlaylistEntry(entry)) {
                invalid++
                null
            } else PlaylistBundleManifestRecord(name, entry)
        }
        return PlaylistBundleManifest(records, invalid)
    }

    private fun isSafePlaylistEntry(entry: String): Boolean =
        entry.startsWith("playlists/") && entry.endsWith(".m3u8", ignoreCase = true) &&
            !entry.contains('\\') && !entry.endsWith('/') && entry.split('/').none { it == ".." || it.isBlank() }
}
