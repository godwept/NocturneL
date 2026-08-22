package ca.stewark.nocturnel

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSiteContractTest {
    private fun repoFile(path: String) = File("..", path)

    private fun read(path: String): String = repoFile(path).readText()

    private fun assertContainsAll(source: String, vararg expected: String) {
        expected.forEach { value -> assertTrue("Missing expected site content: $value", value in source) }
    }

    @Test fun siteConfigurationTargetsTheProjectPagesUrl() {
        val config = read("docs/_config.yml")
        assertContainsAll(
            config,
            "url: \"https://godwept.github.io\"",
            "baseurl: \"/NocturneL\"",
            "support_email: \"nocturnelapp@gmail.com\"",
            "github_url: \"https://github.com/godwept/NocturneL\"",
            "release_status: \"Coming soon on Google Play\"",
            "manual:",
            "output: true",
            "jekyll-seo-tag",
        )
    }

    @Test fun globalNavigationHasOneAuthoritativeOrder() {
        val navigation = read("docs/_data/navigation.yml")
        val labels = Regex("(?m)^- label: (.+)$").findAll(navigation).map { it.groupValues[1] }.toList()
        assertEquals(listOf("Home", "Features", "Screenshots", "Manual", "Privacy", "GitHub"), labels)
        assertContainsAll(
            navigation,
            "url: \"/\"",
            "url: \"/#features\"",
            "url: \"/#screenshots\"",
            "url: \"/manual/\"",
            "url: \"/privacy/\"",
            "url: \"https://github.com/godwept/NocturneL\"",
        )
        assertEquals(1, Regex("(?m)^  external: true$").findAll(navigation).count())
    }

    @Test fun sharedShellIsSemanticBaseAwareAndPrivate() {
        val layout = read("docs/_layouts/default.html")
        assertContainsAll(
            layout,
            "lang=\"{{ site.lang",
            "name=\"viewport\"",
            "{% seo %}",
            "theme-color",
            "skip-link",
            "{% include header.html %}",
            "<main id=\"main-content\"",
            "{% include footer.html %}",
            "assets/css/site.css",
            "assets/js/site.js",
            "relative_url",
        )
        val header = read("docs/_includes/header.html")
        assertContainsAll(header, "site.data.navigation", "aria-current", "relative_url", "aria-expanded")
        val footer = read("docs/_includes/footer.html")
        assertContainsAll(footer, "site.support_email", "/privacy/", "/manual/", "site.github_url")
        val combined = layout + header + footer
        listOf("google-analytics", "googletagmanager", "cookie", "tracking-pixel").forEach {
            assertFalse("Unexpected tracking dependency: $it", combined.contains(it, ignoreCase = true))
        }
    }

    @Test fun themeIncludesResponsiveAccessibleStates() {
        val css = read("docs/assets/css/site.css")
        assertContainsAll(
            css,
            "--background:", "--panel:", "--phosphor:", "--phosphor-dim:", "--amber:",
            "--foreground:", "--muted:", "--border:", "--glow:", "--content-width:", "--space-",
            "body {", ".skip-link", ".site-header", ".site-nav", ".site-footer", ":focus-visible",
            "overflow-wrap", "min-height: 44px", "@media (max-width:",
            "prefers-reduced-motion: reduce", "prefers-contrast: more", ".js .site-nav",
        )
        assertFalse("Remote fonts are outside the approved design", "@import url(" in css)
    }

    @Test fun scriptIsProgressiveAndKeepsNoPersistentState() {
        val script = read("docs/assets/js/site.js")
        assertContainsAll(
            script,
            "classList.add('js')", "aria-expanded", "Escape", "h2[id], h3[id]",
            "manual-content", "heading-anchor",
        )
        listOf("localStorage", "sessionStorage", "document.cookie", "fetch(", "XMLHttpRequest", "sendBeacon").forEach {
            assertFalse("Unexpected browser persistence or network API: $it", it in script)
        }
    }

    @Test fun landingPageCoversTheApprovedProductStory() {
        val landing = read("docs/index.md")
        assertContainsAll(
            landing,
            "layout: home", "# Your music. Your device.", "offline", "terminal-themed Android music player",
            "site.release_status", "Read the manual", "'/manual/' | relative_url", "id=\"features\"",
            "selected folder", "Albums, artists, and search", "background", "editable queue", "visualizers",
            "portable playlists", "No accounts", "No ads", "No analytics", "No telemetry", "internet permission",
            "id=\"screenshots\"", "01-library.png", "02-album.png", "03-now-playing.png", "04-queue.png",
            "Android 12+", "MP3", "M4A", "AAC", "OGG", "Opus", "WAV", "FLAC",
            "device's Android media codecs", "nocturnelapp@gmail.com", "site.github_url",
        )
        assertEquals(1, Regex("(?m)^# ").findAll(landing).count())
        assertFalse("Do not publish a placeholder Play Store link", "play.google.com" in landing)
        listOf("01-library.png", "02-album.png", "03-now-playing.png", "04-queue.png").forEach {
            assertTrue(repoFile("docs/play-store/listing/graphics/phone/$it").isFile)
        }
    }

    @Test fun manualLayoutProvidesOrderedNavigationAndNeighbors() {
        val layout = read("docs/_layouts/manual.html")
        assertContainsAll(
            layout,
            "site.manual | sort: \"nav_order\"", "manual-nav.html", "manual-content",
            "Previous", "Next", "Back to NocturneL", "forloop.first", "forloop.last",
        )
        val navigation = read("docs/_includes/manual-nav.html")
        assertContainsAll(navigation, "aria-label=\"Manual\"", "aria-current=\"page\"", "relative_url")
        val overview = read("docs/manual/index.md")
        assertContainsAll(overview, "permalink: /manual/", "site.manual | sort", "entry.description", "site.support_email")
    }

    @Test fun manualCollectionIsCompleteAndAccurate() {
        val expected = listOf(
            ManualContract("getting-started.md", 10, "/manual/getting-started/", listOf("Android 12", "CHOOSE MUSIC FOLDER", "LIB", "SEA", "ART", "PLY", "NOW", "SET")),
            ManualContract("library.md", 20, "/manual/library/", listOf("GRID", "FLOW", "ARTIST", "TITLE", "YEAR", "MOST PLAYED", "SET COVER", "CHANGE MUSIC FOLDER")),
            ManualContract("playback.md", 30, "/manual/playback/", listOf("Play or pause", "Previous", "Next", "Repeat all", "Repeat one", "lock screen", "audio focus", "restore")),
            ManualContract("queue.md", 40, "/manual/queue/", listOf("ADD QUEUE", "UPCOMING", "drag", "UNDO", "CLEAR UPCOMING", "SHUFFLE", "REPEAT ALL", "QUEUE CHANGED")),
            ManualContract("visualizers.md", 50, "/manual/visualizers/", listOf("Album art", "Circular radar", "Spectrum bars", "CRT EFFECTS", "SIGNAL UNAVAILABLE", "25 ms", "-2000", "+2000")),
            ManualContract("playlists.md", 60, "/manual/playlists/", listOf("CREATE", "RENAME", "DELETE", "M3U", "M3U8", "ZIP", "EXPORT ALL", "unavailable")),
            ManualContract("listening-activity.md", 70, "/manual/listening-activity/", listOf("favorites", "play counts", "MOST PLAYED", "qualified", "CLEAR HISTORY + COUNTS", "resume")),
            ManualContract("settings.md", 80, "/manual/settings/", listOf("CHANGE MUSIC FOLDER", "RESCAN LIBRARY", "CRT EFFECTS", "reduced-motion", "PRIVACY POLICY", "CLEAR HISTORY + COUNTS")),
            ManualContract("formats-and-artwork.md", 90, "/manual/formats-and-artwork/", listOf("MP3", "M4A", "AAC", "OGG", "Opus", "WAV", "FLAC", "cover.jpg", "folder.jpg", "albumart.jpg", "front.jpg")),
            ManualContract("troubleshooting.md", 100, "/manual/troubleshooting/", listOf("No playable albums", "folder access", "notification", "SIGNAL UNAVAILABLE", "QUEUE CHANGED", "nocturnelapp@gmail.com")),
            ManualContract("privacy-and-data.md", 110, "/manual/privacy-and-data/", listOf("no internet permission", "No accounts", "No ads", "No analytics", "cloud backup", "uninstall", "Google Play")),
        )
        val files = repoFile("docs/_manual").listFiles { file -> file.extension == "md" }?.toList().orEmpty()
        assertEquals(expected.size, files.size)
        expected.forEach(::assertManualPage)
        assertEquals(expected.map { it.order }.toSet().size, expected.size)
        assertEquals(expected.map { it.permalink }.toSet().size, expected.size)
    }

    @Test fun policy404AndMetadataUseTheSharedSite() {
        val policy = read("docs/privacy/index.md")
        assertContainsAll(policy, "layout: page", "title:", "description:", "Last updated:", "does not collect")
        val notFound = read("docs/404.html")
        assertContainsAll(notFound, "layout: page", "permalink: /404.html", "404", "Home", "Manual", "Privacy", "GitHub", "relative_url")
        listOf("docs/index.md", "docs/manual/index.md", "docs/privacy/index.md").forEach { path ->
            val page = read(path)
            assertTrue("Missing title in $path", Regex("(?m)^title: .+$").containsMatchIn(page))
            assertTrue("Missing description in $path", Regex("(?m)^description: .+$").containsMatchIn(page))
        }
    }

    @Test fun siteWorkflowBuildsAndCapturesWithoutDeploying() {
        val workflow = read(".github/workflows/site.yml")
        assertContainsAll(
            workflow,
            "pull_request:", "push:", "contents: read", "actions/checkout@v4",
            "actions/configure-pages@v5", "actions/jekyll-build-pages@v1", "source: ./docs",
            "390,844", "1440,1000", "curl", "actions/upload-artifact@v4", "nocturnel-site-previews",
        )
        assertFalse("Validation workflow must not deploy", "actions/deploy-pages" in workflow)
        assertFalse("Validation workflow must not request Pages write access", "pages: write" in workflow)
    }

    @Test fun browserReviewChecklistCoversApprovedRisks() {
        val checklist = read("docs/testing/product-site-checklist.md")
        assertContainsAll(
            checklist,
            "Phone", "Tablet", "Desktop", "Chrome", "Firefox", "Keyboard", "focus",
            "screen reader", "heading", "alt text", "200%", "horizontal overflow",
            "reduced motion", "increased contrast", "JavaScript disabled", "deep link",
            "404", "Lighthouse", "accessibility", "performance", "SEO", "CI screenshots",
        )
    }

    private fun assertManualPage(contract: ManualContract) {
        val source = read("docs/_manual/${contract.file}")
        assertContainsAll(
            source,
            "layout: manual",
            "description:",
            "section:",
            "nav_order: ${contract.order}",
            "permalink: ${contract.permalink}",
        )
        assertTrue("Missing title in ${contract.file}", Regex("(?m)^title: .+$").containsMatchIn(source))
        contract.required.forEach { expected ->
            assertTrue("Missing '$expected' in ${contract.file}", source.contains(expected, ignoreCase = true))
        }
    }

    private data class ManualContract(
        val file: String,
        val order: Int,
        val permalink: String,
        val required: List<String>,
    )
}
