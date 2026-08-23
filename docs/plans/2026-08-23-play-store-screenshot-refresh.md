# Play Store Screenshot Refresh Implementation Plan

**Date:** 2026-08-23  
**Design doc:** docs/specs/2026-08-23-play-store-screenshot-refresh-design.md  
**Status:** Ready for review

## Overview

Replace the retired four-image phone screenshot set with the five supplied captures—Library, Album detail, Spectrum bands, Radar visualizer, and Now Playing album artwork—across the Play Store asset contract, release gate, product website, and GitHub README. Preserve the numeric filename order everywhere, losslessly convert the user-approved captures from 32-bit ARGB to opaque 24-bit RGB PNGs, retain tablet assets unchanged, and extend existing Kotlin contract tests before each content or workflow change.

## Tasks

### Task 1: Require and normalize the five phone screenshots

**Files:** `app/src/test/java/ca/stewark/nocturnel/PlayStoreAssetsTest.kt`, `docs/play-store/listing/graphics/phone/01-library.png`, `docs/play-store/listing/graphics/phone/02-album.png`, `docs/play-store/listing/graphics/phone/03-vis1.png`, `docs/play-store/listing/graphics/phone/04-vis2.png`, `docs/play-store/listing/graphics/phone/05-now-playing-album.png`

**Test first:** Replace the obsolete phone test methods with five required screenshot tests. Rename the phone helper from `assertPendingScreenshot` to `assertRequiredScreenshot` so a missing phone capture fails instead of being skipped; retain `assertPendingScreenshot` for the unchanged tablet captures. Have the required helper verify 1080x1920 dimensions, every decoded pixel is fully opaque, `image.colorModel.hasAlpha()` is false, and `image.colorModel.pixelSize` is 24.

```kotlin
@Test fun phoneLibrary() = assertRequiredScreenshot("phone/01-library.png")
@Test fun phoneAlbum() = assertRequiredScreenshot("phone/02-album.png")
@Test fun phoneSpectrumBands() = assertRequiredScreenshot("phone/03-vis1.png")
@Test fun phoneRadarVisualizer() = assertRequiredScreenshot("phone/04-vis2.png")
@Test fun phoneNowPlayingAlbum() = assertRequiredScreenshot("phone/05-now-playing-album.png")

private fun assertRequiredScreenshot(relativePath: String) {
    val file = File(graphics, relativePath)
    assertTrue("Missing asset: ${file.path}", file.isFile)
    val image = ImageIO.read(file)
    assertEquals("Width for $relativePath", 1080, image.width)
    assertEquals("Height for $relativePath", 1920, image.height)
    val allPixelsOpaque = (0 until image.height).all { y ->
        image.getRGB(0, y, image.width, 1, null, 0, image.width)
            .all { pixel -> pixel ushr 24 == 0xff }
    }
    assertTrue("Transparent pixel in $relativePath", allPixelsOpaque)
    assertFalse("Unexpected alpha channel: $relativePath", image.colorModel.hasAlpha())
    assertEquals("Bit depth for $relativePath", 24, image.colorModel.pixelSize)
}
```

Run the test before conversion. It must fail on the alpha-channel/bit-depth assertions while confirming that no pixel is actually transparent, establishing that dropping the unused alpha channel is safe.

**Implementation:** Re-encode only the five listed phone files with .NET `System.Drawing`: load each PNG, draw it unscaled into a same-size `Format24bppRgb` bitmap, save to a sibling temporary PNG, dispose all image handles, and replace the original with the temporary file. Do not resize, recolor, recompress as JPEG, or touch tablet assets. Use this PowerShell shape for each explicit path:

```powershell
Add-Type -AssemblyName System.Drawing
$screenshotPaths = @(
  'docs/play-store/listing/graphics/phone/01-library.png',
  'docs/play-store/listing/graphics/phone/02-album.png',
  'docs/play-store/listing/graphics/phone/03-vis1.png',
  'docs/play-store/listing/graphics/phone/04-vis2.png',
  'docs/play-store/listing/graphics/phone/05-now-playing-album.png'
)
foreach ($screenshotPath in $screenshotPaths) {
  $sourcePath = (Resolve-Path -LiteralPath $screenshotPath).Path
  $temporaryPath = "$sourcePath.rgb.png"
  $source = [System.Drawing.Bitmap]::FromFile($sourcePath)
  $rgb = New-Object System.Drawing.Bitmap($source.Width, $source.Height, [System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
  $drawing = [System.Drawing.Graphics]::FromImage($rgb)
  try {
    $drawing.DrawImageUnscaled($source, 0, 0)
    $rgb.Save($temporaryPath, [System.Drawing.Imaging.ImageFormat]::Png)
  } finally {
    $drawing.Dispose()
    $rgb.Dispose()
    $source.Dispose()
  }
  Move-Item -LiteralPath $temporaryPath -Destination $sourcePath -Force
}
```

**Verify:** Run `./gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.PlayStoreAssetsTest"`. All five phone tests pass with 1080x1920, fully opaque, non-alpha, 24-bit images; the unchanged tablet tests also pass or retain their existing pending behavior.

---

### Task 2: Update the Play Store graphics contract

**Files:** `app/src/test/java/ca/stewark/nocturnel/PlayStoreAssetsTest.kt`, `docs/play-store/listing/graphics/README.md`

**Test first:** Replace the old phone entries in `assetManifestDocumentsEveryImage` with the five approved filenames. Add an ordered manifest test that collects table rows beginning with `| phone/`, asserts the exact five-name sequence below, asserts exactly five rows, and checks that the final alt-text cell of every row is nonblank and at most 140 characters.

```kotlin
assertEquals(
    listOf(
        "phone/01-library.png",
        "phone/02-album.png",
        "phone/03-vis1.png",
        "phone/04-vis2.png",
        "phone/05-now-playing-album.png",
    ),
    phoneRows.map { row -> row.split('|')[1].trim() },
)
```

Run the targeted test and confirm it fails because the graphics README still documents the retired phone set.

**Implementation:** Replace only the phone rows in `docs/play-store/listing/graphics/README.md`, leaving icon, feature graphic, tablet rows, and the format contract unchanged. Write the rows in this order with these subjects and alt texts:

| File | Subject | Alt text |
|---|---|---|
| `phone/01-library.png` | Populated library | NocturneL album library in a phosphor-green terminal grid |
| `phone/02-album.png` | Album detail | NocturneL album detail with track list and local playback actions |
| `phone/03-vis1.png` | Spectrum bands | NocturneL Now Playing with a green terminal spectrum-band visualizer |
| `phone/04-vis2.png` | Radar visualizer | NocturneL Now Playing with a neon circular radar visualizer |
| `phone/05-now-playing-album.png` | Now Playing album art | NocturneL Now Playing with album artwork and playback controls |

Every row keeps the documented size `1080x1920`.

**Verify:** Run the targeted `PlayStoreAssetsTest`. Confirm the ordered manifest and 140-character checks pass.

---

### Task 3: Update the release asset gate

**Files:** `app/src/test/java/ca/stewark/nocturnel/ReleaseWorkflowContractTest.kt`, `.github/workflows/play-release.yml`

**Test first:** Replace the two retired phone paths in `releaseWorkflowIsManualPublishOnly` with `phone/03-vis1.png`, `phone/04-vis2.png`, and `phone/05-now-playing-album.png`. Add negative assertions that the release workflow no longer contains either retired phone path; do not reject similarly named tablet assets.

```kotlin
assertFalse("Retired phone Now Playing asset remains required", "phone/03-now-playing.png" in release)
assertFalse("Retired phone Queue asset remains required", "phone/04-queue.png" in release)
```

Run the targeted test and confirm it fails because the release workflow still requires the deleted phone files.

**Implementation:** In the `REQUIRED_ASSETS` array under `Require Play Store assets`, preserve icon, feature graphic, and all tablet paths. Replace the two retired phone entries with the three approved visualizer/album-art entries in numeric order.

**Verify:** Run `./gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ReleaseWorkflowContractTest"`. The workflow contract passes and still enforces all unrelated release requirements.

---

### Task 4: Expand the product-site screenshot gallery

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/index.md`, `docs/assets/css/site.css`

**Test first:** Update `landingPageCoversTheApprovedProductStory` to require the five new paths and captions, assert their index positions are strictly increasing, and assert the retired phone paths are absent. Isolate the markup between `id="screenshots"` and the following section; require exactly five `screenshot-card` figures, five unique `alt` values, and every alt value to be nonblank and at most 140 characters. Extend `themeIncludesResponsiveAccessibleStates` to require `.screenshot-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr));` while retaining the existing two-column media rule.

Use these exact paths and captions as the ordered test data:

```kotlin
val screenshots = listOf(
    "01-library.png" to "01 / Library",
    "02-album.png" to "02 / Album detail",
    "03-vis1.png" to "03 / Spectrum bands",
    "04-vis2.png" to "04 / Radar visualizer",
    "05-now-playing-album.png" to "05 / Album artwork",
)
```

Run the targeted test and confirm it fails on the old paths, four-card count, and four-column desktop rule.

**Implementation:** Replace the four figures in `docs/index.md` with five figures in the approved order. Keep explicit `width="1080"`, `height="1920"`, `loading="lazy"`, `relative_url`, and existing card markup. Use the captions above and the same five alt texts defined in Task 2. Change only the base `.screenshot-grid` rule from four to five equal columns; retain the existing two-column rules at 900px and 520px.

**Verify:** Run `./gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ProductSiteContractTest"`. Confirm all five paths, captions, unique alt texts, ordering, and responsive CSS contracts pass.

---

### Task 5: Expand the GitHub landing-page screenshot table

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `README.md`

**Test first:** Add `githubLandingPageShowsApprovedScreenshotSequence`. Read the section between `## Screenshots` and `## Highlights`; require the five column labels and image paths in strict order, require exactly five `<img>` tags, require unique nonblank alt text no longer than 140 characters, require `width="180"` on each image, and reject the two retired phone paths.

```kotlin
val expectedLabels = listOf("Library", "Album", "Spectrum bands", "Radar visualizer", "Album artwork")
val expectedPaths = listOf(
    "01-library.png", "02-album.png", "03-vis1.png", "04-vis2.png", "05-now-playing-album.png",
)
```

Run the targeted test and confirm it fails against the existing four-column table.

**Implementation:** Replace the README screenshot table with five columns using the labels above and the same file order and alt text as Tasks 2 and 4. Set each image width to 180 so the five-column presentation remains compact on GitHub. Do not change the surrounding highlights or other README content.

**Verify:** Run the targeted `ProductSiteContractTest`. Confirm the table has five ordered images and all accessibility assertions pass.

---

### Task 6: Run regression and presentation checks

**Files:** No planned file changes; fix only files named in Tasks 1–5 if verification exposes a defect.

**Test first:** No new test is introduced here; this task runs all tests that preceded the corresponding implementation changes.

**Implementation:** Search public/configuration surfaces for stale phone references while excluding historical design and plan documents. Inspect the final diff and confirm tablet graphics, feature graphic, icon, unrelated website copy, and application source are unchanged. Do not upload to Google Play or deploy GitHub Pages.

**Verify:** Run:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.PlayStoreAssetsTest" --tests "ca.stewark.nocturnel.ReleaseWorkflowContractTest" --tests "ca.stewark.nocturnel.ProductSiteContractTest"
./gradlew.bat :app:testDebugUnitTest
rg -n "phone/03-now-playing\.png|phone/04-queue\.png" README.md docs/index.md docs/play-store/listing/graphics/README.md .github/workflows/play-release.yml app/src/test/java/ca/stewark/nocturnel
git diff --check
git status --short
```

The `rg` command must return no matches. Review the next successful `Product site validation` workflow artifact `nocturnel-site-previews`, checking both `landing-phone.png` and `landing-desktop.png`: all five captures are present in order, captions remain legible, cards do not overflow, and the desktop view uses a balanced five-column row. If a local Jekyll/browser toolchain becomes available first, perform the equivalent 390x844 and 1440x1000 review locally.

## Definition of Done

- [ ] All tasks completed in order
- [ ] Every content, workflow, and binary-format change was preceded by its failing contract test
- [ ] All five phone screenshots are 1080x1920 opaque 24-bit RGB PNGs
- [ ] Play Store contract, release gate, product website, and GitHub README use the same five-file order
- [ ] All alt text is unique per surface, descriptive, and no longer than 140 characters
- [ ] No retired phone screenshot references remain in active public/configuration/test surfaces
- [ ] Targeted and complete unit-test suites pass
- [ ] `git diff --check` passes and no unplanned files are modified
- [ ] Phone and desktop site previews show a readable, overflow-free five-image gallery
- [ ] No tablet assets, feature graphic, icon, application behavior, deployment state, or Play Console content changed

