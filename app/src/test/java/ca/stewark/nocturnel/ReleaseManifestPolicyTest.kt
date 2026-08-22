package ca.stewark.nocturnel

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseManifestPolicyTest {
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test fun backupIsDisabledAndInternetPermissionIsAbsent() {
        assertTrue("android:allowBackup=\"false\"" in manifest)
        assertFalse("android.permission.INTERNET" in manifest)
    }

    @Test fun mainActivityIsRestrictedToPortraitOrientation() {
        assertTrue("android:screenOrientation=\"portrait\"" in manifest)
    }
}
