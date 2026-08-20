package ca.stewark.nocturnel.library

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanProgressTest {
    @Test
    fun indexingProgressRetainsCompletedAndTotal() {
        assertEquals(ScanProgress.Indexing(completed = 7, total = 10), ScanProgress.Indexing(7, 10))
    }

    @Test
    fun discoveryProgressHasNoPretendPercentage() {
        assertEquals(ScanProgress.Discovering, ScanProgress.Discovering)
    }
}
