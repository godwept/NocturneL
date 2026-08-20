package ca.stewark.nocturnel

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityTest {
    @Test
    fun applicationUsesNocturneLPackageName() {
        assertEquals("ca.stewark.nocturnel", BuildConfig.APPLICATION_ID)
    }

    @Test fun activityUsesSdkAwareNotificationPermissionPolicy() {
        val source = File("src/main/java/ca/stewark/nocturnel/MainActivity.kt").readText()
        assertTrue("NotificationPermissionPolicy.shouldRequest" in source)
        assertTrue("Build.VERSION.SDK_INT" in source)
    }
}
