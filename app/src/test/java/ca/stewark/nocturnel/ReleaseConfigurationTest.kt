package ca.stewark.nocturnel

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseConfigurationTest {
    private val buildFile = File("build.gradle.kts").readText()

    @Test fun releaseSdkAndVersionArePinned() {
        assertTrue("compileSdk = 36" in buildFile)
        assertTrue("minSdk = 31" in buildFile)
        assertTrue("targetSdk = 36" in buildFile)
        assertTrue("versionCode = releaseVersionCode" in buildFile)
        assertTrue("versionName = releaseVersionName" in buildFile)
        assertTrue("val releaseVersionCode = 5" in buildFile)
        assertTrue("val releaseVersionName = \"0.1.0\"" in buildFile)
    }

    @Test fun releaseBuildIsOptimized() {
        assertTrue("release {" in buildFile)
        assertTrue("isMinifyEnabled = true" in buildFile)
        assertTrue("isShrinkResources = true" in buildFile)
        assertTrue("proguard-android-optimize.txt" in buildFile)
        assertTrue("proguard-rules.pro" in buildFile)
    }

    @Test fun releaseVersionCanBeReadDeterministically() {
        assertTrue("printReleaseVersion" in buildFile)
        assertTrue("println(\"${'$'}releaseVersionName ${'$'}releaseVersionCode\")" in buildFile)
    }

    @Test fun composeInstrumentedTestsIncludeTheirHostActivity() {
        assertTrue("debugImplementation(libs.androidx.compose.ui.test.manifest)" in buildFile)
    }
}
