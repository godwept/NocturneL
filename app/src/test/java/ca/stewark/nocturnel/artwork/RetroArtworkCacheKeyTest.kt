package ca.stewark.nocturnel.artwork

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RetroArtworkCacheKeyTest {
    @Test fun `key is stable and includes every transformation input`() {
        assertEquals(RetroArtworkCacheKey("cover").toString(), RetroArtworkCacheKey("cover").toString())
        assertNotEquals(RetroArtworkCacheKey("one").toString(), RetroArtworkCacheKey("two").toString())
        assertNotEquals(RetroArtworkCacheKey("one", "a").toString(), RetroArtworkCacheKey("one", "b").toString())
        assertNotEquals(RetroArtworkCacheKey("one", paletteSize = 8).toString(), RetroArtworkCacheKey("one", paletteSize = 16).toString())
    }
}
