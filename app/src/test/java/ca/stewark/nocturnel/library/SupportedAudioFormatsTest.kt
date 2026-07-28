package ca.stewark.nocturnel.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedAudioFormatsTest {
    @Test
    fun recognizesCommonLocalAudioExtensionsCaseInsensitively() {
        assertTrue(SupportedAudioFormats.isCandidateAudioFile("Track.MP3"))
        assertTrue(SupportedAudioFormats.isCandidateAudioFile("Track.flac"))
        assertFalse(SupportedAudioFormats.isCandidateAudioFile("cover.jpg"))
    }
}
