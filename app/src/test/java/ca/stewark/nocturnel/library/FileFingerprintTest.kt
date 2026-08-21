package ca.stewark.nocturnel.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileFingerprintTest {
    @Test fun matchingReliableFingerprintCanBeReused() {
        assertTrue(FileFingerprint(42, 1_000).matches(FileFingerprint(42, 1_000)))
    }

    @Test fun unknownOrInvalidValuesAreNeverReusable() {
        assertFalse(FileFingerprint(null, 1_000).matches(FileFingerprint(null, 1_000)))
        assertFalse(FileFingerprint(-1, 1_000).matches(FileFingerprint(-1, 1_000)))
        assertFalse(FileFingerprint(42, null).matches(FileFingerprint(42, null)))
        assertFalse(FileFingerprint(42, 0).matches(FileFingerprint(42, 0)))
    }

    @Test fun changedSizeOrModifiedTimeIsNotReusable() {
        assertFalse(FileFingerprint(42, 1_000).matches(FileFingerprint(43, 1_000)))
        assertFalse(FileFingerprint(42, 1_000).matches(FileFingerprint(42, 1_001)))
    }
}
