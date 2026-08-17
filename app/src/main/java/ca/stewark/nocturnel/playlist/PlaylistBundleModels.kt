package ca.stewark.nocturnel.playlist

const val PLAYLIST_BUNDLE_FORMAT = "nocturnel-playlists"
const val PLAYLIST_BUNDLE_VERSION = 1
const val PLAYLIST_BUNDLE_MANIFEST = "nocturnel-playlists.json"
const val MAX_BUNDLE_PLAYLISTS = 1_000
const val MAX_BUNDLE_PLAYLIST_BYTES = 4 * 1024 * 1024
const val MAX_BUNDLE_UNCOMPRESSED_BYTES = 64L * 1024 * 1024

data class PlaylistBundlePlaylist(val name: String, val paths: List<String>)
data class PlaylistBundleDecodeResult(
    val playlists: List<PlaylistBundlePlaylist>,
    val skippedPlaylists: Int,
    val skippedTracks: Int,
)
data class PlaylistBundleLimits(
    val maxPlaylists: Int = MAX_BUNDLE_PLAYLISTS,
    val maxPlaylistBytes: Int = MAX_BUNDLE_PLAYLIST_BYTES,
    val maxTotalBytes: Long = MAX_BUNDLE_UNCOMPRESSED_BYTES,
)

class UnsupportedPlaylistBundleException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)
