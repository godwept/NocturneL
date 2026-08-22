# NocturneL Product Site and Manual Implementation Plan

**Date:** 2026-08-22  
**Design doc:** docs/specs/2026-08-22-nocturnel-product-site-manual-design.md  
**Status:** Ready for review

## Overview

Replace the minimal GitHub Pages content in `docs/` with a custom, responsive Jekyll product site and a complete source-accurate manual. The implementation will reuse the existing screenshots and branding, publish beneath `/NocturneL/`, use shared Liquid layouts and a small progressive-enhancement script, and add repository contract tests plus an isolated Jekyll CI build with desktop and phone preview captures.

## Tasks

### Task 1: Establish the product-site contract test and Jekyll metadata

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_config.yml`, `.gitignore`

**Test first:** Create `ProductSiteContractTest` using the existing `File("..", path)` repository-test convention. Add helpers `repoFile`, `read`, and `assertContainsAll`, then add a test that requires `url: "https://godwept.github.io"`, `baseurl: "/NocturneL"`, the support email, repository URL, release status, and manual collection output in `_config.yml`.

```kotlin
@Test fun siteConfigurationTargetsTheProjectPagesUrl() {
    val config = read("docs/_config.yml")
    assertContainsAll(config,
        "url: \"https://godwept.github.io\"",
        "baseurl: \"/NocturneL\"",
        "support_email: \"nocturnelapp@gmail.com\"",
        "release_status: \"Coming soon on Google Play\"",
        "manual:",
        "output: true",
    )
}
```

**Implementation:** Expand `_config.yml` with the approved title, description, canonical `url`, `baseurl`, `repository`, `github_url`, `support_email`, `release_status`, `logo`, `social_image`, `lang`, `plugins: [jekyll-seo-tag]`, Kramdown heading IDs, and the `_manual` output collection. Add `_site/` and `docs/_site/` to `.gitignore` for local Jekyll output.

**Verify:** Run `./gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ProductSiteContractTest"`. The new test passes.

---

### Task 2: Define authoritative global navigation

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_data/navigation.yml`

**Test first:** Add a test requiring exactly six ordered navigation labels—Home, Features, Screenshots, Manual, Privacy, GitHub—and the expected destinations. Assert that the first five internal destinations begin with `/` and that only GitHub is marked external.

**Implementation:** Create `_data/navigation.yml` with entries for `/`, `/#features`, `/#screenshots`, `/manual/`, `/privacy/`, and `https://github.com/godwept/NocturneL`. Include an `external: true` flag only on GitHub so templates can render external-link affordances consistently.

**Verify:** Run the targeted `ProductSiteContractTest`; all tests pass.

---

### Task 3: Build the shared semantic page shell

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_layouts/default.html`, `docs/_includes/header.html`, `docs/_includes/footer.html`

**Test first:** Add a test that requires the default layout to contain a skip link, `lang`, viewport metadata, header, main, and footer landmarks, and base-aware stylesheet/script links. Require the header to loop through `site.data.navigation`, use `relative_url` for internal links, expose `aria-current`, and label external links. Require the footer to use the configured support email and privacy URL.

**Implementation:** Create the default HTML document and shared header/footer includes. The header contains a linked NocturneL wordmark, desktop/mobile navigation, a real menu button that is hidden unless JavaScript activates it, and current-page semantics. The footer contains the offline/no-tracking statement, support email, Privacy, Manual, and GitHub links. Wrap page content in `<main id="main-content">` and load `assets/css/site.css` plus deferred `assets/js/site.js` through `relative_url`.

**Verify:** Run the targeted contract test and inspect the Liquid includes for duplicated hard-coded `/NocturneL` prefixes; tests pass and no duplicates remain.

---

### Task 4: Add the terminal-inspired visual foundation

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/assets/css/site.css`

**Test first:** Add a source-guard test requiring named CSS custom properties for background, panel, phosphor, dim phosphor, amber, foreground, muted text, border, glow, maximum content width, and spacing. Require styling for `body`, links, buttons, focus-visible, skip link, site header, navigation, page shell, and footer.

**Implementation:** Create `site.css` with a dark terminal palette derived from the app, system sans-serif body text, a readable system monospace stack for labels/headings, restrained glow and grid texture, consistent spacing/radii, a centered content width, strong focus rings, and conventional link underlines. Do not load remote fonts, trackers, or third-party CSS.

**Verify:** Run the targeted contract test; open the stylesheet and confirm all colors are referenced through tokens rather than repeated literals.

---

### Task 5: Add responsive and accessibility states

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/assets/css/site.css`

**Test first:** Add a test requiring media rules for narrow screens, `prefers-reduced-motion: reduce`, and `prefers-contrast: more`; require `overflow-wrap`, a minimum interactive target size, visible focus styling, and a `.js`-scoped collapsed-navigation rule.

**Implementation:** Extend the stylesheet with responsive header, hero, feature-grid, screenshot-grid, and manual layouts. Prevent horizontal overflow for long headings and URLs, keep controls at least 44px high, make the manual sidebar collapse into an in-flow contents panel, disable smooth scrolling/transitions/glow movement under reduced motion, and strengthen borders/text under increased contrast. With no `.js` class, mobile navigation remains visible and usable.

**Verify:** Run the targeted contract test; all CSS accessibility guards pass.

---

### Task 6: Implement progressive navigation and heading anchors

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/assets/js/site.js`, `docs/_includes/header.html`

**Test first:** Add a source-guard test requiring the script to add a `js` class, toggle `aria-expanded`, close navigation after an internal link is chosen or Escape is pressed, and add shareable links only to manual `h2[id]` and `h3[id]` headings. Assert that it contains no storage, cookie, analytics, fetch, or network APIs.

**Implementation:** Write a dependency-free deferred script that enhances the mobile menu and appends accessible `#` anchor links to manual headings. Preserve ordinary links and heading IDs as the no-JavaScript fallback; keep all state in memory for the current page only.

**Verify:** Run the targeted contract test; then temporarily disable the script reference and confirm from the source structure that navigation and heading content remain reachable.

---

### Task 7: Create the landing-page hero

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_layouts/home.html`, `docs/index.md`, `docs/assets/css/site.css`

**Test first:** Add a test requiring `docs/index.md` to use the home layout, contain one H1, explain that NocturneL is an offline terminal-themed Android music player for locally owned music, render the configured release status as non-link text, and link “Read the manual” to `/manual/` via `relative_url`. Assert there is no Play Store URL.

**Implementation:** Create the home layout as a thin specialization of the default layout. Replace the minimal index with front matter and a semantic hero containing a terminal-style eyebrow, concise headline, supporting copy, a primary manual link, non-interactive coming-soon badge, and the app icon/feature artwork with explicit dimensions and descriptive alt text.

**Verify:** Run the targeted contract test; all hero/status assertions pass.

---

### Task 8: Add product feature and privacy sections

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/index.md`, `docs/assets/css/site.css`

**Test first:** Add a test requiring stable `features` and `privacy` section IDs and source-accurate copy covering the selected local folder, albums/artists/search, background playback, editable queue, visualizers, portable playlists, local listening data, and the absence of accounts, ads, analytics, telemetry, and internet permission.

**Implementation:** Add a brief product-introduction block, a six-card feature grid, and a visually distinct privacy section. Use concise marketing copy consistent with `README.md` and the manifest; link the detailed privacy policy instead of duplicating it.

**Verify:** Run the targeted contract test; compare claims against `README.md` and `app/src/main/AndroidManifest.xml`.

---

### Task 9: Add screenshots, requirements, formats, and support

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/index.md`, `docs/assets/css/site.css`

**Test first:** Add a test requiring a `screenshots` section with all four existing phone image paths and unique alt text, Android 12+, the seven supported extensions, the codec-variation caveat, support email, Privacy, Manual, and secondary GitHub links.

**Implementation:** Add an accessible four-item screenshot gallery using the existing Library, Album, Now Playing, and Queue captures. Add compact requirements/formats and support sections. Style screenshots as device-like panels without embedding them in inaccessible ornamental frames.

**Verify:** Run the targeted contract test and confirm each referenced PNG exists under `docs/play-store/listing/graphics/phone/`.

---

### Task 10: Create the manual layout and collection navigation

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_layouts/manual.html`, `docs/_includes/manual-nav.html`, `docs/assets/css/site.css`

**Test first:** Add a test requiring the manual layout to sort `site.manual` by `nav_order`, render a labeled manual navigation, identify the current topic, expose previous/next links with first/last guards, include a back-to-site link, and wrap article content in a `.manual-content` container.

**Implementation:** Create the manual layout on top of the default layout and a shared navigation include. On wide screens render a sticky sidebar; on narrow screens render the same links as an in-flow contents block. Compute previous/next items from the sorted collection, never from duplicated hard-coded arrays.

**Verify:** Run the targeted contract test; manually trace first, middle, and last collection entries through the Liquid guards.

---

### Task 11: Create the manual overview

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/manual/index.md`, `docs/assets/css/site.css`

**Test first:** Add a test requiring `/manual/`, a clear introduction, a generated loop over the sorted manual collection, topic descriptions, and direct support/privacy links.

**Implementation:** Create the manual overview using the general page layout. Explain the manual’s audience and Android 12+ scope, then render every topic as a linked title and description sourced from collection front matter.

**Verify:** Run the targeted contract test; the overview contract passes.

---

### Task 12: Document getting started

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/getting-started.md`

**Test first:** Add `assertManualPage` to the contract-test helpers, validating `layout`, `title`, `description`, `section`, numeric `nav_order`, and exact `permalink`. Add a test for the getting-started page requiring Android 12, notification permission behavior, “Choose Music Folder,” selected-folder descendants, scan progress/cancellation, and the six app navigation abbreviations.

**Implementation:** Create nav order 10 at `/manual/getting-started/`. Explain requirements, first launch, Android’s folder picker, initial scanning, cancellation, notification permission on supported Android versions, and the LIB/SEA/ART/PLY/NOW/SET navigation labels. State that NocturneL reads only the chosen folder and descendants.

**Verify:** Run the targeted contract test; the page metadata and required guidance pass.

---

### Task 13: Document library browsing

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/library.md`

**Test first:** Add a page contract for nav order 20 and `/manual/library/` requiring albums, artists, search groups, Grid/Flow, Artist/Title/Year/Most Played sorting, album play/shuffle/add-queue actions, favorites, manual cover selection, rescanning, and folder-change data loss confirmation.

**Implementation:** Document the library landing page, grid and cover-flow switching, sort cycling, album/artist detail, local grouped search, album/track actions, cover selection, rescan/cancel behavior, access-loss recovery, and the warning that changing the music folder clears favorites, history, counts, and resume state.

**Verify:** Run the targeted contract test and compare terminology with `LibraryLandingScreen.kt`, `SearchScreen.kt`, `AlbumDetailScreen.kt`, and `SettingsScreen.kt`.

---

### Task 14: Document playback

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/playback.md`

**Test first:** Add a page contract for nav order 30 requiring play/pause, previous/next, seek, shuffle, repeat off/all/one, favorite, play count, background playback, lock-screen/media controls, audio focus, and playback restoration.

**Implementation:** Explain starting tracks/albums/playlists, Now Playing metadata and seek bar, transport controls, shuffle/repeat cycling, favorites/play counts, background and lock-screen control, expected audio-focus behavior, and restoration after normal interruption or app recreation. Avoid promising identical codec behavior on every device.

**Verify:** Run the targeted contract test and cross-check claims with `NowPlayingScreen.kt`, `NocturneLPlaybackService.kt`, and playback policy tests.

---

### Task 15: Document queue management

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/queue.md`

**Test first:** Add a page contract for nav order 40 requiring add-track/add-album/add-playlist, current versus upcoming items, jump, drag reorder, accessible move controls, remove/undo, clear upcoming confirmation, shuffle interaction, repeat-all interaction, and stale-queue retry guidance.

**Implementation:** Explain opening the queue from Now Playing, adding content from supported screens, jumping to an occurrence, reordering, removing and undoing, clearing only upcoming items, and the notices produced when shuffle/repeat settings must change or the live queue changed during editing.

**Verify:** Run the targeted contract test and compare wording with `QueueEditorScreen.kt`, `QueueEditingPolicy.kt`, and `PlaybackConnection.kt`.

---

### Task 16: Document visualizers and sync controls

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/visualizers.md`

**Test first:** Add a page contract for nav order 50 requiring Album Art, Circular Radar, Spectrum Bars, tap-to-cycle behavior, CRT Effects, signal-unavailable state, sync minus/plus/reset controls, 25 ms increments, and the -2000 to +2000 ms range.

**Implementation:** Explain the three display modes, how tapping cycles modes, live-signal availability, afterglow/CRT effects, Android reduced-motion override, and visualizer sync adjustment. Explain that negative/positive adjustment should be tuned by observation without claiming device-independent latency.

**Verify:** Run the targeted contract test and cross-check numeric limits with `VisualizerSyncOffset.kt`.

---

### Task 17: Document playlists and portable files

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/playlists.md`

**Test first:** Add a page contract for nav order 60 requiring create, open, play, rename, delete, add/remove/filter/reorder tracks, add album, add queue, M3U/M3U8 import/export, ZIP export-all/import, unavailable entries, and user-chosen document locations.

**Implementation:** Document the playlist index and editor, album-to-playlist picker, ordering/removal, playable versus unavailable entries, single-playlist M3U8 export, M3U/M3U8 import, and all-playlist ZIP backup/restore. State that Android’s document picker controls import/export locations and that exported files remain after uninstall until the user removes them.

**Verify:** Run the targeted contract test and compare with `PlaylistsScreen.kt`, `PlaylistDetailScreen.kt`, and the playlist codec/service tests.

---

### Task 18: Document listening activity accurately

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/listening-activity.md`

**Test first:** Add a page contract for nav order 70 requiring track/album favorites, visible play counts, Most Played sorting, local qualified-play history, clear-history-and-counts confirmation, and the fact that favorites and resume state are preserved by that clearing action.

**Implementation:** Explain where favorite toggles and play counts appear, how counts support Most Played sorting, that qualified plays/history remain local, and how Settings clears history and counts while preserving favorites and resume. Do not claim that the current app exposes a dedicated browsable history or recents screen.

**Verify:** Run the targeted contract test and confirm the page does not contain claims such as “open the History screen” or “browse Recent tracks.”

---

### Task 19: Document settings

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/settings.md`

**Test first:** Add a page contract for nav order 80 requiring change music folder, rescan/cancel, CRT effects, Android reduced-motion behavior, privacy policy, clear history/counts, preserved favorites/resume, and folder-change clearing behavior.

**Implementation:** Document every control currently shown by `SettingsScreen`, including confirmation and warning behavior. Explain which library display/sort and visualizer-sync preferences persist even though their controls live on Library/Now Playing rather than Settings.

**Verify:** Run the targeted contract test and compare labels with `SettingsScreen.kt` and `TerminalPreferencesRepository.kt`.

---

### Task 20: Document formats, metadata, and artwork

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/formats-and-artwork.md`

**Test first:** Add a page contract for nav order 90 requiring MP3, M4A, AAC, OGG, Opus, WAV, FLAC, the Android codec caveat, embedded metadata, path fallbacks, manual/embedded/folder/placeholder artwork precedence, and all four recognized cover filenames.

**Implementation:** Explain candidate file extensions versus actual device playback support, metadata fallback behavior for missing tags, artwork resolution order, accepted folder-cover names (`cover.jpg`, `folder.jpg`, `albumart.jpg`, `front.jpg`), and per-album manual cover selection.

**Verify:** Run the targeted contract test and compare the lists with `SupportedAudioFormats.kt`, `MetadataFallbacks.kt`, and `ArtworkResolver.kt`.

---

### Task 21: Add troubleshooting and FAQ guidance

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/troubleshooting.md`

**Test first:** Add a page contract for nav order 100 requiring guidance for empty libraries, lost folder access, failed/slow scans, unsupported or unplayable files, missing metadata/artwork, playback notification permission, unavailable playlist entries, visualizer signal/sync, queue-changed notices, and escalation to the support email.

**Implementation:** Write symptom/action troubleshooting entries using the app’s actual recovery paths: rescan, reselect the folder, verify extension/device codec, set a cover, grant notification permission, repair playlist paths through import/source availability, reset sync, retry queue edits, and contact support with Android/device/app-version context. Do not invent cache clearing, network fixes, or server status steps.

**Verify:** Run the targeted contract test; every troubleshooting action maps to an existing app behavior.

---

### Task 22: Document privacy and local-data lifecycle

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/privacy-and-data.md`

**Test first:** Add a page contract for nav order 110 requiring no internet permission, accounts, ads, analytics, telemetry, or remote crash reporting; local catalog/playlists/history/favorites/settings; Android cloud backup disabled; clear-data/uninstall behavior; exported-file retention; Google Play purchase separation; and links to the formal policy and support.

**Implementation:** Create a plain-language manual chapter that explains accessed data, local persistence, clearing history/counts, changing folders, Android app-data clearing, uninstall behavior, exported files, and the boundary between NocturneL and future Google Play purchase processing. Link rather than duplicate the formal legal policy.

**Verify:** Run the targeted contract test and compare claims with `AndroidManifest.xml` and `docs/privacy/index.md`.

---

### Task 23: Style the formal policy and add a useful 404 page

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/privacy/index.md`, `docs/404.html`, `docs/_layouts/page.html`, `docs/assets/css/site.css`

**Test first:** Add a test requiring the privacy policy to use the shared page layout without changing its approved policy wording. Require `404.html` to use permalink `/404.html`, identify the error, and include base-aware links to Home, Manual, Privacy, and GitHub.

**Implementation:** Add front matter to the existing policy and create a general page layout that inherits the shared shell. Add the branded 404 page and compact error-panel styling. Preserve all substantive privacy-policy text and its last-updated date.

**Verify:** Run `ProductSiteContractTest` and the existing `PlayStoreMetadataTest`; both pass.

---

### Task 24: Complete SEO and social-sharing metadata

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_layouts/default.html`, `docs/index.md`, `docs/manual/index.md`, `docs/privacy/index.md`, `docs/_manual/getting-started.md`, `docs/_manual/library.md`, `docs/_manual/playback.md`, `docs/_manual/queue.md`, `docs/_manual/visualizers.md`, `docs/_manual/playlists.md`, `docs/_manual/listening-activity.md`, `docs/_manual/settings.md`, `docs/_manual/formats-and-artwork.md`, `docs/_manual/troubleshooting.md`, `docs/_manual/privacy-and-data.md`

**Test first:** Add a test requiring `{% seo %}`, canonical URL output, theme color, favicon/app-icon links, and page-specific `title`/`description` front matter. Require the landing page to identify the existing feature graphic as its social image and assert that no analytics or cookie scripts appear in the layout.

**Implementation:** Finish the `<head>` with `jekyll-seo-tag`, canonical and theme metadata, app icons, and social image data derived from the existing feature graphic. Ensure every public page has a concise unique title and description; use configured defaults only where appropriate.

**Verify:** Run the targeted contract test and search `docs/` for analytics, tracking pixels, cookie banners, or placeholder store URLs; none are present.

---

### Task 25: Enforce complete manual metadata and routes

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`

**Test first:** Add a final collection-level test that enumerates `docs/_manual/*.md`, requires exactly eleven pages, asserts unique `nav_order` and `permalink` values, requires orders 10 through 110 in increments of 10, and matches the approved route list. Add a source-link validator that checks local Markdown/HTML image references and internal page destinations against the known landing, manual, privacy, and 404 routes.

**Implementation:** Correct only metadata, navigation ordering, or internal references exposed by the new failing tests. Do not add new chapters or routes.

**Verify:** Run the complete `ProductSiteContractTest`; all collection and link checks pass.

---

### Task 26: Add Jekyll build and visual-preview CI

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `.github/workflows/site.yml`

**Test first:** Add a workflow contract test requiring pull-request and push triggers for site-related paths, read-only contents permission, checkout, `actions/configure-pages@v5`, `actions/jekyll-build-pages@v1` with `source: ./docs`, phone and desktop headless-Chrome captures, curl readiness checking, and `actions/upload-artifact@v4`. Assert that the workflow has no deployment permission or `actions/deploy-pages` step.

**Implementation:** Create a validation-only workflow that builds `docs/` into `_site`, serves a preview beneath `_preview/NocturneL/`, waits for it with bounded curl retries, captures the landing page at 390×844 and 1440×1000, and uploads both PNGs as `nocturnel-site-previews`. Keep publication on the repository’s existing `/docs` GitHub Pages source rather than introducing a second deployment path.

**Verify:** Run the targeted contract test. Validate the YAML structure, then confirm the first pushed workflow run builds Jekyll and uploads both screenshots.

---

### Task 27: Record browser and accessibility review

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/testing/product-site-checklist.md`

**Test first:** Add a documentation contract requiring checklist entries for phone/tablet/desktop, Chrome/Firefox, keyboard and focus, screen-reader landmarks/headings/alt text, 200% text, narrow-screen overflow, reduced motion, increased contrast, JavaScript disabled, deep manual links, 404, Lighthouse accessibility/performance/SEO, and review of both CI screenshots.

**Implementation:** Create the repeatable product-site checklist with URL/build under test, tester/date fields, pass/fail boxes, notes, and the exact review cases from the approved design. Include a check that every manual instruction still matches the current app release.

**Verify:** Run the targeted contract test, then execute the checklist against a successful GitHub Pages preview or local Jekyll build.

---

### Task 28: Run the full regression suite and audit scope

**Files:** No planned file changes; fix only files named in earlier tasks if verification exposes a defect.

**Test first:** No new test is introduced in this task; it executes all tests written before their corresponding implementation throughout Tasks 1–27.

**Implementation:** Run formatting/whitespace checks and inspect the final diff. Confirm the Android application source and existing Play Store artifacts were not modified, the Google Play status remains non-interactive, and no out-of-scope accounts, forms, analytics, search, blog, localization, downloads, or framework dependencies were added.

**Verify:** Run:

```powershell
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :app:lintRelease
git diff --check
git status --short
```

Then complete `docs/testing/product-site-checklist.md` against the built site and confirm the site workflow succeeds.

## Definition of Done

- [ ] All tasks completed in order
- [ ] Every production change was preceded by its failing contract test
- [ ] All unit tests pass (`./gradlew.bat :app:testDebugUnitTest`)
- [ ] Release lint passes (`./gradlew.bat :app:lintRelease`)
- [ ] The Jekyll site workflow builds successfully and produces phone/desktop previews
- [ ] The browser and accessibility checklist is complete
- [ ] No unplanned files modified
- [ ] The feature behaves exactly as described in the approved design document
