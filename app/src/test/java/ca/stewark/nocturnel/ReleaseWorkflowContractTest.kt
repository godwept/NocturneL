package ca.stewark.nocturnel

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseWorkflowContractTest {
    private fun workflow(path: String) = File("../.github/workflows/$path").readText()

    @Test fun ordinaryCiBuildsTheReleaseVariantWithoutSecrets() {
        val ci = workflow("android.yml")
        assertTrue("lintRelease" in ci)
        assertTrue("bundleRelease" in ci)
        assertFalse("NOCTURNEL_UPLOAD_" in ci)
    }

    @Test fun releaseWorkflowIsManualPublishOnly() {
        val release = workflow("play-release.yml")
        listOf(
            "play/*", "contents: read", "java-version: '17'", "quality:", "device-tests:",
            "package:", "environment: play-release", "testDebugUnitTest",
            "validateDebugScreenshotTest", "lintRelease", "assembleDebugAndroidTest",
            "bundleRelease", "api-level: [31, 36]", "connectedDebugAndroidTest",
            ":app:printReleaseVersion", "validate-release-version.sh", "bundletool-all-1.18.3.jar",
            "a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29",
            "bundletool.jar\" validate", "bundletool.jar\" dump manifest",
            "ca.stewark.nocturnel", "android.permission.INTERNET", "allowBackup",
            "NOCTURNEL_UPLOAD_KEYSTORE_BASE64", "NOCTURNEL_UPLOAD_KEY_ALIAS",
            "NOCTURNEL_UPLOAD_STORE_PASSWORD", "NOCTURNEL_UPLOAD_KEY_PASSWORD",
            "jarsigner", "-storepass:env", "-keypass:env", "-verify", "-strict",
            "sha256sum", "mapping.txt", "retention-days: 30", "if: always()",
        ).forEach { expected -> assertTrue("Missing workflow contract: $expected", expected in release) }
        assertTrue("needs: [quality, device-tests]" in release)
        assertFalse("NOCTURNEL_UPLOAD_" in release.substringBefore("\n  package:"))
        assertTrue(Regex("(?m)^    environment: play-release\\r?$").findAll(release).count() == 1)
        assertFalse("playDeveloperServiceAccount" in release)
        assertFalse("publishBundle" in release)
        val uploadedPaths = Regex("(?m)^\\s+path:\\s+(.+)$")
            .findAll(release)
            .map { it.groupValues[1].trim() }
            .toList()
        assertFalse(uploadedPaths.any { it.endsWith(".jks") || it.endsWith(".keystore") })
    }
}
