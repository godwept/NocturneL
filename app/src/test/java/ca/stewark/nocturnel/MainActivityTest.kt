package ca.stewark.nocturnel

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityTest {
    @Test
    fun applicationUsesNocturneLPackageName() {
        assertEquals("ca.stewark.nocturnel", BuildConfig.APPLICATION_ID)
    }
}
