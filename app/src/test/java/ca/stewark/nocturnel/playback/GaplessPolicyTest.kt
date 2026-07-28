package ca.stewark.nocturnel.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GaplessPolicyTest {
    @Test fun unknownMetadataUsesNormalTransition() = assertFalse(GaplessPolicy.permitsTransition(false, true))
    @Test fun confirmedMetadataAllowsSameAlbumTransition() = assertTrue(GaplessPolicy.permitsTransition(true, true))
}
