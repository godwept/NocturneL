package ca.stewark.nocturnel.ui.playlist

import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.playlist.M3u8Codec
import ca.stewark.nocturnel.playlist.PlaylistBundleDecodeResult
import ca.stewark.nocturnel.playlist.PlaylistBundlePlaylist
import ca.stewark.nocturnel.playlist.uniqueImportedPlaylistName
import kotlinx.coroutines.CancellationException

sealed interface PlaylistImportPayload {
    data class Standalone(val suggestedName: String, val text: String) : PlaylistImportPayload
    data class Bundle(val result: PlaylistBundleDecodeResult) : PlaylistImportPayload
}

data class PlaylistImportSummary(
    val importedPlaylists: Int,
    val importedTracks: Int,
    val skippedPlaylists: Int,
    val skippedTracks: Int,
) {
    val message: String
        get() = "Imported $importedPlaylists playlist(s), $importedTracks track(s); " +
            "skipped $skippedPlaylists playlist(s), $skippedTracks track(s)."
}

data class PlaylistExportSummary(val exportedPlaylists: Int) {
    val message: String get() = "Exported $exportedPlaylists playlist(s)."
}

internal object PlaylistTransferMessages {
    const val IMPORT_FAILED = "Playlist import failed"
    const val EXPORT_FAILED = "Playlist export failed"
    const val IMPORT_CANCELLED = "Playlist import cancelled"
    const val EXPORT_CANCELLED = "Playlist export cancelled"
    const val PLAYLIST_EXPORTED = "Playlist exported"
}

class PlaylistImportCommand(
    private val existingNames: suspend () -> List<String>,
    private val knownPaths: suspend () -> Set<String>,
    private val createWithEntries: suspend (String, List<String>) -> Unit,
) {
    suspend fun import(payload: PlaylistImportPayload): PlaylistImportSummary = when (payload) {
        is PlaylistImportPayload.Standalone -> importStandalone(payload)
        is PlaylistImportPayload.Bundle -> importBundle(payload.result)
    }

    private suspend fun importStandalone(payload: PlaylistImportPayload.Standalone): PlaylistImportSummary {
        val parsed = M3u8Codec.parse(payload.text, knownPaths())
        val name = uniqueImportedPlaylistName(payload.suggestedName, existingNames().toMutableSet())
        createWithEntries(name, parsed.paths)
        return PlaylistImportSummary(1, parsed.paths.size, 0, parsed.skipped.size)
    }

    private suspend fun importBundle(result: PlaylistBundleDecodeResult): PlaylistImportSummary {
        val usedNames = existingNames().toMutableSet()
        var importedPlaylists = 0
        var importedTracks = 0
        var skippedPlaylists = result.skippedPlaylists

        result.playlists.forEach { playlist ->
            val name = uniqueImportedPlaylistName(playlist.name, usedNames)
            try {
                createWithEntries(name, playlist.paths)
                importedPlaylists++
                importedTracks += playlist.paths.size
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                skippedPlaylists++
            }
        }

        return PlaylistImportSummary(
            importedPlaylists,
            importedTracks,
            skippedPlaylists,
            result.skippedTracks,
        )
    }
}

class PlaylistExportCommand(
    private val allPlaylists: suspend () -> List<PlaylistEntity>,
    private val paths: suspend (Long) -> List<String>,
) {
    suspend fun collect(): List<PlaylistBundlePlaylist> = allPlaylists().map { playlist ->
        PlaylistBundlePlaylist(playlist.name, paths(playlist.id))
    }
}
