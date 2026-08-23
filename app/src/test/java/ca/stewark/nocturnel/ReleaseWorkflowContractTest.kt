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
            "Require Play Store assets",
            "validateDebugScreenshotTest", "lintRelease", "assembleDebugAndroidTest",
            "bundleRelease", "api-level: [31, 36]", "connectedDebugAndroidTest",
            "id: instrumented-tests", "continue-on-error: true",
            ".instrumented-tests-completed", ".instrumented-tests-failed",
            "Verify instrumented test result", "adb shell getprop ro.build.version.sdk",
            ":app:printReleaseVersion", "validate-release-version.sh", "bundletool-all-1.18.3.jar",
            "a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29",
            "bundletool.jar\" validate", "bundletool.jar\" dump manifest",
            "ca.stewark.nocturnel", "android.permission.INTERNET", "allowBackup",
            "NOCTURNEL_UPLOAD_KEYSTORE_BASE64", "NOCTURNEL_UPLOAD_KEY_ALIAS",
            "NOCTURNEL_UPLOAD_STORE_PASSWORD", "NOCTURNEL_UPLOAD_KEY_PASSWORD",
            "jarsigner", "-storepass:env", "-keypass:env", "-verify", "-strict",
            "sha256sum", "mapping.txt", "retention-days: 30", "if: always()",
            "phone/01-library.png", "phone/02-album.png", "phone/03-vis1.png",
            "phone/04-vis2.png", "phone/05-now-playing-album.png",
        ).forEach { expected -> assertTrue("Missing workflow contract: $expected", expected in release) }
        val retiredNowPlaying = listOf("phone", "03-now-playing.png").joinToString("/")
        val retiredQueue = listOf("phone", "04-queue.png").joinToString("/")
        assertFalse("Retired phone Now Playing asset remains required", retiredNowPlaying in release)
        assertFalse("Retired phone Queue asset remains required", retiredQueue in release)
        listOf(
            "tablet/01-library.png",
            "tablet/02-album.png",
            "tablet/03-now-playing.png",
            "tablet/04-queue.png",
        ).forEach { retiredTablet ->
            assertFalse("Tablet screenshot remains required: $retiredTablet", retiredTablet in release)
        }
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
