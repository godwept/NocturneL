package ca.stewark.nocturnel.ui.playlist

import ca.stewark.nocturnel.data.entity.PlaylistEntity
import ca.stewark.nocturnel.playlist.PlaylistBundleDecodeResult
import ca.stewark.nocturnel.playlist.PlaylistBundlePlaylist
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistTransferCommandTest {
    @Test fun bundleKeepsMissingPathsRenamesConflictsAndContinuesAfterFailure() = runTest {
        val created = mutableListOf<Pair<String, List<String>>>()
        val command = PlaylistImportCommand(
            existingNames = { listOf("Mix", "mix (2)") },
            knownPaths = { emptySet() },
            createWithEntries = { name, paths ->
                if (name == "Broken") error("write failed")
                created += name to paths
            },
        )
        val summary = command.import(
            PlaylistImportPayload.Bundle(
                PlaylistBundleDecodeResult(
                    listOf(
                        PlaylistBundlePlaylist("Mix", listOf("missing.flac")),
                        PlaylistBundlePlaylist("Broken", listOf("b.flac")),
                        PlaylistBundlePlaylist("Mix", emptyList()),
                    ),
                    skippedPlaylists = 1,
                    skippedTracks = 2,
                ),
            ),
        )
        assertEquals(listOf("Mix (3)", "Mix (4)"), created.map { it.first })
        assertEquals(listOf("missing.flac"), created.first().second)
        assertEquals("Imported 2 playlist(s), 1 track(s); skipped 2 playlist(s), 2 track(s).", summary.message)
    }

    @Test fun standaloneKeepsExistingKnownPathFiltering() = runTest {
        var created: Pair<String, List<String>>? = null
        val command = PlaylistImportCommand({ emptyList() }, { setOf("known.flac") }) { name, paths -> created = name to paths }
        val result = command.import(PlaylistImportPayload.Standalone("Import", "known.flac\nmissing.flac"))
        assertEquals("Import", created!!.first)
        assertEquals(listOf("known.flac"), created!!.second)
        assertEquals(1, result.skippedTracks)
    }

    @Test fun exportCollectsEmptyAndDuplicateNamedPlaylists() = runTest {
        val command = PlaylistExportCommand(
            allPlaylists = { listOf(PlaylistEntity(1, "Mix", 1), PlaylistEntity(2, "Mix", 2)) },
            paths = { id -> if (id == 1L) listOf("a.flac") else emptyList() },
        )
        val playlists = command.collect()
        assertEquals(listOf("Mix", "Mix"), playlists.map { it.name })
        assertEquals(emptyList<String>(), playlists[1].paths)
        assertEquals("Exported 2 playlist(s).", PlaylistExportSummary(playlists.size).message)
    }
}
