package ca.stewark.nocturnel.playback

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueEditingWiringTest {
    @Test fun mediaItemsCarryOccurrenceMetadataAndTimelineEditsPersist() {
        val connection = File("src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt").readText()
        val service = File("src/main/java/ca/stewark/nocturnel/playback/NocturneLPlaybackService.kt").readText()
        assertTrue("QUEUE_OCCURRENCE_ID" in connection)
        assertTrue("QUEUE_OCCURRENCE_ID" in service)
        assertTrue("QUEUE_DURATION_MS" in connection)
        assertTrue("QUEUE_DURATION_MS" in service)
        assertTrue("Player.EVENT_TIMELINE_CHANGED" in service)
        assertTrue("replaceMediaItems" in connection)
        assertTrue("playWhenReady = false" in connection)
    }

    @Test fun appWiresDedicatedQueueEditorAndAllQueueEntryPoints() {
        val app = File("src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt").readText()
        val playlists = File("src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistsScreen.kt").readText()
        assertTrue("QueueEditorScreen" in app)
        assertTrue("playback::moveQueueOccurrence" in app)
        assertTrue("playback::expireQueueUndo" in app)
        assertTrue("onPlayAlbumNext = playback::playNext" in app)
        assertTrue("onPlayNext = { playback.playNext" in app)
        assertTrue("player.playNext" in playlists)
        assertTrue("player.addToQueue" in playlists)
    }
}
