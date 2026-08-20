package ca.stewark.nocturnel.ui

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyPolicyWiringTest {
    @Test fun settingsOpensThePublicPrivacyPolicy() {
        val source = File("src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt").readText()
        assertTrue("https://godwept.github.io/NocturneL/privacy/" in source)
        assertTrue("LocalUriHandler.current" in source)
        assertTrue("onOpenPrivacyPolicy" in source)
        assertTrue("openUri(PRIVACY_POLICY_URL)" in source)
    }
}
