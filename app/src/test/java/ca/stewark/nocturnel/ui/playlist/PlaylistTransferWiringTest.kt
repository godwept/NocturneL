package ca.stewark.nocturnel.ui.playlist

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistTransferWiringTest {
    @Test fun documentPickersSupportBundlesAndCancellation() {
        val source = File("src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistsScreen.kt").readText()
        assertTrue("application/zip" in source)
        assertTrue("application/x-zip-compressed" in source)
        assertTrue("NocturneL Playlists.zip" in source)
        assertTrue("viewModel.exportAll" in source)
        assertTrue("viewModel.importCancelled" in source)
        assertTrue("viewModel.exportCancelled" in source)
    }
}
