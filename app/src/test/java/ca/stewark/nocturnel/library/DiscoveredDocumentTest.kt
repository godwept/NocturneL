package ca.stewark.nocturnel.library

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoveredDocumentTest {
    @Test fun discoveredAudioCarriesTheFingerprintWithoutAndroidDocumentObjects() {
        val document = DiscoveredDocument("Artist/Album/01.mp3", "content://track/1", "01.mp3", 42, 1_000)
        assertEquals(FileFingerprint(42, 1_000), document.fingerprint)
    }
}
