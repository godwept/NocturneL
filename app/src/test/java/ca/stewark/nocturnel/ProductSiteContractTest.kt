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
            ".manual-pager a { display: block; height: 100%;",
            ".feature-card h3 { margin: var(--space-3) 0 var(--space-2);",
            ".screenshot-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr));",
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
            "id=\"screenshots\"", "01-library.png", "02-album.png", "03-vis1.png", "04-vis2.png",
            "05-now-playing-album.png", "01 / Library", "02 / Album detail", "03 / Spectrum bands",
            "04 / Radar visualizer", "05 / Album artwork",
            "Android 12+", "MP3", "M4A", "AAC", "OGG", "Opus", "WAV", "FLAC",
            "device's Android media codecs", "nocturnelapp@gmail.com", "site.github_url",
        )
        assertEquals(1, Regex("(?m)^# ").findAll(landing).count())
        assertFalse("Do not publish a placeholder Play Store link", "play.google.com" in landing)
        val screenshots = listOf(
            "01-library.png" to "01 / Library",
            "02-album.png" to "02 / Album detail",
            "03-vis1.png" to "03 / Spectrum bands",
            "04-vis2.png" to "04 / Radar visualizer",
            "05-now-playing-album.png" to "05 / Album artwork",
        )
        val screenshotPositions = screenshots.map { (path, caption) ->
            assertTrue(repoFile("docs/play-store/listing/graphics/phone/$path").isFile)
            val pathPosition = landing.indexOf(path)
            assertTrue("Missing screenshot path: $path", pathPosition >= 0)
            assertTrue("Missing screenshot caption: $caption", caption in landing)
            pathPosition
        }
        assertTrue("Screenshot paths are not in approved order", screenshotPositions.zipWithNext().all { (first, second) -> first < second })
        assertFalse("Retired phone Now Playing reference remains", "03-now-playing.png" in landing)
        assertFalse("Retired phone Queue reference remains", "04-queue.png" in landing)
        val screenshotSection = landing.substringAfter("id=\"screenshots\"")
            .substringBefore("<section class=\"site-section\" aria-labelledby=\"requirements-title\">")
        assertEquals(5, Regex("class=\"screenshot-card terminal-panel\"").findAll(screenshotSection).count())
        val altTexts = Regex("<img[^>]+alt=\"([^\"]+)\"")
            .findAll(screenshotSection)
            .map { it.groupValues[1] }
            .toList()
        assertEquals(5, altTexts.size)
        assertEquals(5, altTexts.toSet().size)
        altTexts.forEach { altText ->
            assertTrue("Screenshot alt text must not be blank", altText.isNotBlank())
            assertTrue("Screenshot alt text exceeds 140 characters: $altText", altText.length <= 140)
        }
    }

    @Test fun githubLandingPageShowsApprovedScreenshotSequence() {
        val screenshots = read("README.md")
            .substringAfter("## Screenshots")
            .substringBefore("## Highlights")
        val tableRows = screenshots.lineSequence().filter { it.startsWith("|") }.toList()
        assertEquals(
            listOf("Library", "Album", "Spectrum bands", "Radar visualizer", "Album artwork"),
            tableRows.first().trim('|').split('|').map(String::trim),
        )
        val expectedPaths = listOf(
            "01-library.png",
            "02-album.png",
            "03-vis1.png",
            "04-vis2.png",
            "05-now-playing-album.png",
        )
        val positions = expectedPaths.map { path ->
            screenshots.indexOf(path).also { position ->
                assertTrue("Missing README screenshot path: $path", position >= 0)
            }
        }
        assertTrue("README screenshots are not in approved order", positions.zipWithNext().all { (first, second) -> first < second })
        assertFalse("Retired phone Now Playing reference remains", "03-now-playing.png" in screenshots)
        assertFalse("Retired phone Queue reference remains", "04-queue.png" in screenshots)
        val imageTags = Regex("<img[^>]+>").findAll(screenshots).map { it.value }.toList()
        assertEquals(5, imageTags.size)
        imageTags.forEach { tag -> assertTrue("README screenshot width must be 180: $tag", "width=\"180\"" in tag) }
        val altTexts = imageTags.map { tag ->
            Regex("alt=\"([^\"]+)\"").find(tag)?.groupValues?.get(1).orEmpty()
        }
        assertEquals(5, altTexts.toSet().size)
        altTexts.forEach { altText ->
            assertTrue("README screenshot alt text must not be blank", altText.isNotBlank())
            assertTrue("README screenshot alt text exceeds 140 characters: $altText", altText.length <= 140)
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
            ManualContract("getting-started.md", 10, "/manual/getting-started/", listOf("Android 12", "CHOOSE MUSIC FOLDER", "five labels", "LIB", "SEA", "ART", "PLY", "NOW", "Settings", "gear")),
            ManualContract("library.md", 20, "/manual/library/", listOf("GRID", "FLOW", "ARTIST", "TITLE", "YEAR", "MOST PLAYED", "SET COVER", "CHANGE MUSIC FOLDER")),
            ManualContract("playback.md", 30, "/manual/playback/", listOf("Play or pause", "Previous", "Next", "Repeat all", "Repeat one", "lock screen", "audio focus", "restore")),
            ManualContract("queue.md", 40, "/manual/queue/", listOf("ADD QUEUE", "UPCOMING", "drag", "UNDO", "CLEAR UPCOMING", "SHUFFLE", "REPEAT ALL", "QUEUE CHANGED")),
            ManualContract("visualizers.md", 50, "/manual/visualizers/", listOf("Album art", "Circular radar", "Spectrum bars", "Frequency grid", "CRT EFFECTS", "SIGNAL UNAVAILABLE", "25 ms", "-2000", "+2000")),
            ManualContract("playlists.md", 60, "/manual/playlists/", listOf("CREATE", "RENAME", "DELETE", "M3U", "M3U8", "ZIP", "EXPORT ALL", "unavailable")),
            ManualContract("listening-activity.md", 70, "/manual/listening-activity/", listOf("favorites", "play counts", "MOST PLAYED", "qualified", "CLEAR HISTORY + COUNTS", "resume")),
            ManualContract("settings.md", 80, "/manual/settings/", listOf("CHANGE MUSIC FOLDER", "RESCAN LIBRARY", "CRT EFFECTS", "reduced-motion", "COLOR THEME", "GREEN TERMINAL", "AMBER TERMINAL", "BLUE TERMINAL", "'80S SYNTHWAVE", "'90S NEON", "album covers", "glow", "FONT PRESET", "CLASSIC", "MAINFRAME", "PIXEL", "MODERN", "immediately", "offline", "PRIVACY POLICY", "CLEAR HISTORY + COUNTS")),
            ManualContract("formats-and-artwork.md", 90, "/manual/formats-and-artwork/", listOf("MP3", "M4A", "AAC", "OGG", "Opus", "WAV", "FLAC", "cover.jpg", "folder.jpg", "albumart.jpg", "front.jpg")),
            ManualContract("troubleshooting.md", 100, "/manual/troubleshooting/", listOf("No playable albums", "folder access", "notification", "SIGNAL UNAVAILABLE", "QUEUE CHANGED", "nocturnelapp@gmail.com")),
            ManualContract("privacy-and-data.md", 110, "/manual/privacy-and-data/", listOf("no internet permission", "No accounts", "No ads", "No analytics", "cloud backup", "uninstall", "Google Play")),
        )
        val files = repoFile("docs/_manual").listFiles { file -> file.extension == "md" }?.toList().orEmpty()
        assertEquals(expected.size, files.size)
        expected.forEach(::assertManualPage)
        assertEquals(expected.map { it.order }.toSet().size, expected.size)
        assertEquals(expected.map { it.permalink }.toSet().size, expected.size)
        val gettingStarted = read("docs/_manual/getting-started.md")
        assertFalse("Getting started must not describe six navigation labels", "six labels" in gettingStarted)
        assertFalse("SET must not remain a primary navigation entry", "**SET** —" in gettingStarted)
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
