package ca.stewark.nocturnel.artwork

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkSizingTest {
    @Test fun `large artwork is bounded before palette transformation`() {
        assertEquals(
            512 to 384,
            ArtworkSizing.transformedDimensions(width = 4_000, height = 3_000),
        )
    }
}
