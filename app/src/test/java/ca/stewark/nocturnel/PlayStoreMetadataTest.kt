package ca.stewark.nocturnel

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayStoreMetadataTest {
    private fun repoFile(path: String) = File("..", path)

    @Test fun privacyPolicyIsComplete() {
        val policy = repoFile("docs/privacy/index.md").readText()
        listOf(
            "Privacy Policy",
            "NocturneL",
            "mathew.stewart@gmail.com",
            "does not collect",
            "does not share",
            "user-selected",
            "retained",
            "delete",
            "backup",
            "13",
        ).forEach { expected -> assertTrue("Missing privacy text: $expected", expected in policy) }
    }

    @Test fun githubPagesRoutesToPrivacyPolicy() {
        assertTrue(repoFile("docs/_config.yml").isFile)
        val landing = repoFile("docs/index.md").readText()
        assertTrue("/NocturneL/privacy/" in landing)
    }

    @Test fun englishListingFitsPlayLimits() {
        val listing = repoFile("docs/play-store/listing/en-US.md").readText()
        val fields = parseFields(listing)
        assertTrue(fields.getValue("Title") == "NocturneL")
        assertTrue(fields.getValue("Short description").length in 1..80)
        assertTrue(fields.getValue("Full description").length in 1..4_000)
        assertTrue(fields.getValue("Release notes").isNotBlank())
    }

    @Test fun declarationsMatchApprovedRelease() {
        val declarations = repoFile("docs/play-store/declarations.md").readText()
        listOf(
            "CAD $1.99",
            "CA, US, GB, IE, AU, NZ",
            "Music & Audio",
            "13–15, 16–17, 18+",
            "No ads",
            "No app account",
            "Unrestricted",
            "No data collected or shared",
            "https://godwept.github.io/NocturneL/privacy/",
            "mathew.stewart@gmail.com",
        ).forEach { expected -> assertTrue("Missing declaration: $expected", expected in declarations) }
    }

    @Test fun releaseDocumentationCoversRequiredOperations() {
        val signing = repoFile("docs/release/signing.md").readText()
        listOf(
            "Play App Signing", "upload key", "RSA", "4096", "10000",
            "NOCTURNEL_UPLOAD_KEYSTORE_BASE64", "NOCTURNEL_UPLOAD_KEY_ALIAS",
            "NOCTURNEL_UPLOAD_STORE_PASSWORD", "NOCTURNEL_UPLOAD_KEY_PASSWORD",
            "fingerprint", "reset",
        ).forEach { expected -> assertTrue("Missing signing guidance: $expected", expected in signing) }

        val runbook = repoFile("docs/release/play-store-runbook.md").readText()
        listOf(
            "versionCode", "play/", "checksum", "Internal testing", "pre-launch",
            "Closed testing", "production access", "Production", "halt", "higher version",
            "debug",
        ).forEach { expected -> assertTrue("Missing runbook guidance: $expected", expected in runbook) }

        val privacyAudit = repoFile("docs/release/privacy-audit.md").readText()
        listOf(
            "dependencies", "merged manifest", "permissions", "exported", "backup",
            "Data safety", "privacy", "airplane mode",
        ).forEach { expected -> assertTrue("Missing privacy audit item: $expected", expected in privacyAudit) }

        val closedTest = repoFile("docs/release/closed-test-guide.md").readText()
        listOf(
            "12 testers", "14 continuous days", "15–20", "Google Account", "promo code",
            "mathew.stewart@gmail.com", "Do not commit tester email addresses", "feedback",
        ).forEach { expected -> assertTrue("Missing closed-test guidance: $expected", expected in closedTest) }
    }

    @Test fun deviceChecklistsCoverReleaseRisks() {
        val combined = listOf(
            repoFile("docs/testing/pixel-7-release-checklist.md").readText(),
            repoFile("docs/testing/android-12-release-checklist.md").readText(),
        ).joinToString("\n")
        listOf(
            "Internal testing", "fresh", "upgrade", "airplane mode", "background",
            "lock-screen", "notification", "audio focus", "folder", "scan", "playlist",
            "process death", "restart", "PRIVACY POLICY",
        ).forEach { expected -> assertTrue("Missing device check: $expected", expected in combined) }
    }

    private fun parseFields(markdown: String): Map<String, String> {
        val fields = linkedMapOf<String, String>()
        var current: String? = null
        val buffer = StringBuilder()
        fun flush() {
            current?.let { fields[it] = buffer.toString().trim() }
            buffer.clear()
        }
        markdown.lineSequence().forEach { line ->
            if (line.startsWith("## ")) {
                flush()
                current = line.removePrefix("## ").trim()
            } else if (current != null) {
                buffer.appendLine(line)
            }
        }
        flush()
        return fields
    }
}
