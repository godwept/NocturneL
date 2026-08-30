# Cover Flow Active Cover Implementation Plan

**Date:** 2026-08-30
**Design doc:** `docs/specs/2026-08-30-cover-flow-active-cover-design.md`
**Status:** Ready for review

## Overview

Replace the current separated `240dp` cover-flow row with a responsive overlapping snapping reel. The active cover will use approximately 84% of the available width, never exceed `340dp`, and also respect the available reel height; the immediate neighbors will be flat, smaller, dimmed, and exposed by 10–15%. Existing selection, hard stops, metadata, favorites, effects behavior, and tap-to-center/tap-to-open behavior will remain intact.

## Tasks

### Task 1: Add responsive cover-size geometry

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/library/CoverFlowLayoutTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/CoverFlowLayout.kt`

**Test first:**

Create `CoverFlowLayoutTest` and add `coverSizeUsesWidthHeightAndMaximumConstraints`. Assert that a pure `coverFlowCoverSize(width, height)` function returns:

```kotlin
assertEquals(336f, coverFlowCoverSize(400f, 600f), 0.01f) // 84% width
assertEquals(340f, coverFlowCoverSize(412f, 600f), 0.01f) // maximum
assertEquals(240f, coverFlowCoverSize(412f, 240f), 0.01f) // height constraint
assertEquals(0f, coverFlowCoverSize(0f, 600f), 0.01f)
```

Run the focused test and confirm it fails because the helper does not exist.

**Implementation:**

Create `CoverFlowLayout.kt` in the existing library package. Add internal constants for the approved width fraction (`0.84f`) and maximum size (`340f` in density-independent units), then implement `coverFlowCoverSize(availableWidth, availableHeight)` as the non-negative minimum of 84% width, available height, and the maximum. Keep this helper free of Compose types so it remains a fast JVM unit test.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ui.library.CoverFlowLayoutTest"`. The new test passes.

---

### Task 2: Add overlap-stride geometry

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/library/CoverFlowLayoutTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/CoverFlowLayout.kt`

**Test first:**

Add `overlapStrideExposesApprovedNeighborFraction`. For a `340f` active cover, `0.76f` neighbor scale, and the initial `0.15f` exposure target, call `coverFlowItemStride`. Reconstruct the visible neighbor width with:

```kotlin
val neighborWidth = coverSize * neighborScale
val exposedWidth = stride + neighborWidth / 2f - coverSize / 2f
val exposedFraction = exposedWidth / neighborWidth
```

Assert that `exposedFraction` is between `0.10f` and `0.15f`, and that `stride` is positive but smaller than `coverSize`. Add a second case at a `268.8f` narrow-screen cover size to prove the geometry scales proportionally.

Run the focused test and confirm it fails because the stride helper does not exist.

**Implementation:**

Add constants for the initial neighbor scale (`0.76f`), opacity (`0.50f`), and exposure (`0.15f`). Implement `coverFlowItemStride(coverSize, neighborScale, exposedFraction)` from the centered overlap geometry:

```text
stride = activeHalf - neighborHalf + exposedNeighborWidth
```

Add `coverFlowItemSpacing` that returns `stride - coverSize`; this will be a negative value supplied to the reel arrangement. Validate inputs with coercion rather than throwing during layout.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ui.library.CoverFlowLayoutTest"`. All layout tests pass.

---

### Task 3: Add continuous cover visual-state geometry

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/library/CoverFlowLayoutTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/CoverFlowLayout.kt`

**Test first:**

Add tests named `visualStateEmphasizesCenterAndDimsNeighbors` and `visualStateInterpolatesDuringDragAndHidesDistantItems`. Cover these cases:

- Distance `0f`: scale `1f`, alpha `1f`, and the highest stacking emphasis.
- Distance `1f`: scale `0.76f`, alpha `0.50f`, and lower stacking emphasis.
- Distance `0.5f`: scale and alpha are halfway between the center and neighbor values.
- Absolute distance `2f` or greater: alpha `0f` and non-interactive.
- Negative and positive distances with the same magnitude produce identical scale and alpha.

Also test a `coverFlowDistanceFromCenter(viewportStart, viewportEnd, itemOffset, itemSize, stride)` helper so an item centered in the viewport returns `0f` and adjacent item centers return approximately `-1f` and `1f`.

Run the focused test and confirm the new cases fail.

**Implementation:**

Add an internal `CoverFlowVisualState` value type containing `scale`, `alpha`, `stackingOrder`, and `interactive`. Implement the two pure helpers so emphasis interpolates continuously between the active and adjacent states. Fade items beyond the immediate-neighbor range to zero by an absolute distance of `2f`, preventing additional resting slivers while avoiding abrupt appearance during a drag.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ui.library.CoverFlowLayoutTest"`. All geometry and interpolation tests pass.

---

### Task 4: Apply responsive width and height sizing to the reel

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreen.kt`

**Test first:**

Add `activeCoverUsesResponsiveMaximum`. Render the existing three-album fixture with Beta selected inside a test wrapper constrained to `412dp` wide and tall enough for the cap. Assert that `cover-flow-cover-beta` has a rendered width of `340dp`. Render again at `320dp` width and assert a width near `268.8dp`. Keep the selected album title and favorite control assertions to prove the metadata remains present.

Run the instrumentation test and confirm the current `240dp` implementation fails the width assertions.

**Implementation:**

In the reel's existing `BoxWithConstraints`, replace `minOf(maxWidth * 0.62f, 240.dp)` with a call to `coverFlowCoverSize(maxWidth.value, maxHeight.value).dp`. Continue calculating symmetric content padding from `(maxWidth - coverSize) / 2`, so `scrollToItem`, snapping, and the selected cover remain centered. Do not change `AlbumCoverFlowScreen` parameters or the metadata column.

**Verify:** With an emulator or device available, run `.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.library.AlbumCoverFlowScreenTest`. The responsive-size test and existing screen tests pass.

---

### Task 5: Apply overlapping reel spacing and stacking

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreen.kt`

**Test first:**

Add `layersPreviousAndNextCoversBehindActiveCover`. With Beta selected, fetch the rendered bounds for Alpha, Beta, and Gamma and assert:

```text
Alpha center < Beta center < Gamma center
Alpha right > Beta left
Gamma left < Beta right
Beta rendered width > Alpha rendered width
Beta rendered width > Gamma rendered width
```

These assertions prove both neighbors overlap the active cover and are scaled down. Run the instrumentation test and confirm the current spaced row fails the overlap assertions.

**Implementation:**

In `AlbumCoverFlowScreen.kt`:

- Replace the positive `Arrangement.spacedBy(TerminalDimensions.sm)` spacing with `coverFlowItemSpacing(coverSize.value).dp`.
- Apply `zIndex` so the selected cover draws above the existing smaller neighbors.
- Preserve the existing selected border, scale/alpha behavior, accessibility description, snapping, and selection reconciliation for this step.

**Verify:** Run the focused connected instrumentation command from Task 4. The overlap test and all pre-existing cover-flow tests pass.

---

### Task 6: Connect continuous visual states to scrolling

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreen.kt`

**Test first:**

Add `scrollingTransfersEmphasisToCenteredAlbum`. Begin with Beta selected, swipe toward Gamma, wait for snapping, then compare bounds and descriptions: Gamma must become the full-size selected cover, Beta must become the smaller neighbor, and the metadata must show Gamma. Run it with effects enabled and confirm the screen does not retain stale scale or stacking after the selection changes.

**Implementation:**

For each laid-out item, derive signed distance from the viewport center using `state.layoutInfo`, the item bounds, and `coverFlowDistanceFromCenter`; fall back to `index - selectedIndex` until bounds exist. Convert that distance through `coverFlowVisualState` and apply its scale, alpha, and stacking order. Fully faded distant items must not accept clicks or appear as visible accessibility targets. Use animated scale/alpha targets only when `effectsEnabled` is true; apply them immediately when false. Remove the old binary selected/unselected target calculations, but retain border selection and all callbacks.

**Verify:** Run the focused connected instrumentation command from Task 4. The emphasis-transfer test, the overlap test, and existing selection tests pass.

---

### Task 7: Verify exposed-neighbor touch behavior

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreen.kt`

**Test first:**

Add `touchingExposedNeighborCentersBeforeOpening`. Start with Beta selected, obtain Beta and Gamma's rendered bounds, and use a root-level touch click at the midpoint of Gamma's portion extending beyond Beta's right edge. Assert that Gamma becomes selected and that the opened-album list remains empty. Touch the center of Gamma after settling and assert that only then is `gamma` added to the opened list.

Run the test to catch incorrect stacking or a neighbor whose exposed region is not clickable.

**Implementation:**

Adjust modifier ordering only as required by the failing test: item stacking must be established before pointer dispatch, the active cover must own its full visible area, and the neighbor's existing click action must remain reachable in its exposed strip. Do not add separate arrow buttons, invisible navigation overlays, or wrap-around behavior.

**Verify:** Run the focused connected instrumentation command from Task 4. Both physical-touch behavior and the existing semantic `performClick` test pass.

---

### Task 8: Cover endpoints, small collections, and compact height

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/library/AlbumCoverFlowScreenTest.kt`

**Test first:**

Extend the test fixture helper so it can accept a supplied album list, initial selection, effects flag, and test-container size. Add these tests without changing production behavior:

- `singleAlbumHasNoSidePlaceholders`: one active cover is displayed, its metadata is visible, and no other cover tags exist.
- `twoAlbumsKeepHardStopsAndOnlyAvailableNeighbor`: Alpha at the first endpoint has only Beta on its right; selecting Beta leaves only Alpha on its left; repeated outward swipes do not wrap.
- `compactPortraitKeepsMetadataAndControlsVisible`: at `320 x 640dp`, the selected cover, album title, artist, play count, and favorite control are all displayed.
- Preserve the existing rapid repeated-swipe hard-stop test for the three-album fixture.

The tests should fail if overlap logic creates duplicate placeholders, wraps endpoints, or allows the cover to displace metadata.

**Implementation:**

No production change is expected. If a test exposes a defect, make the smallest correction in `AlbumCoverFlowScreen.kt` or `CoverFlowLayout.kt` consistent with the approved hard-stop and responsive-size rules; do not introduce special-case duplicated albums.

**Verify:** Run the focused connected instrumentation command from Task 4. All cover-flow screen tests pass with effects both enabled and disabled by the fixture cases.

---

### Task 9: Declare endpoint and compact screenshot previews

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`

**Test first:**

Add two annotated preview functions alongside the existing middle-selection previews, calling a not-yet-implemented `initialIndex` argument:

- `Cover flow endpoint`, `412 x 915dp`, effects enabled, initial index `0`.
- `Cover flow compact endpoint`, `320 x 640dp`, effects disabled, initial last index.

Run screenshot validation and confirm compilation fails because `coverFlowPreview` does not yet accept `initialIndex`.

**Implementation:**

Change the private `coverFlowPreview` fixture to accept an initial index while retaining `1` as the default for the existing previews, then add the two annotated preview functions. Do not alter unrelated preview fixtures.

**Verify:** Run `.\gradlew.bat validateDebugScreenshotTest`. The task should compile all preview declarations and fail only because the planned cover-flow references are new or changed.

---

### Task 10: Update and inspect cover-flow references

**Files:** `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/CoverFlowPreview_Cover flow_8bcff85e_0.png`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/CoverFlowEffectsOffPreview_Cover flow effects off_fdc20fc8_0.png`, and the two new plugin-generated cover-flow reference PNGs in that same directory

**Test first:**

Run `.\gradlew.bat validateDebugScreenshotTest` and retain the expected report showing only the changed and missing cover-flow references.

**Implementation:**

Run `.\gradlew.bat updateDebugScreenshotTest` to regenerate the affected cover-flow references. Inspect all four cover-flow images and confirm:

- The standard active cover reaches the intended large presentation.
- Only 10–15% of each available neighbor is exposed.
- Neighbors are flat, smaller, dimmed, and behind the active cover.
- Endpoint previews have an open unavailable side and do not wrap.
- Compact metadata and controls remain fully visible.
- Effects-off retains the same layout without decorative transition state.

If tuning is required, adjust only the neighbor scale, opacity, or exposure constants within the approved design ranges, rerun the geometry tests, and regenerate the same references. Treat any tuning as part of this task; do not alter screen structure or interactions.

**Verify:** Run `.\gradlew.bat validateDebugScreenshotTest`. All screenshot tests pass, and `git status --short` shows no unrelated screenshot reference changes.

---

### Task 11: Run the project verification gate

**Files:** No additional files expected.

**Test first:**

Review `git diff -- app/src/main app/src/test app/src/androidTest app/src/screenshotTest app/src/screenshotTestDebug/reference` and confirm every changed file is named by this plan and every production behavior has a preceding test.

**Implementation:**

Make no feature additions during this task. Fix only failures caused by the planned cover-flow change.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat validateDebugScreenshotTest
.\gradlew.bat assembleDebugAndroidTest
```

If an emulator or device is available, also run the focused connected cover-flow test command from Task 4. Confirm the pre-existing user-owned deletions under `docs/plans` and `docs/specs` were not restored or otherwise modified.

## Definition of Done

- [ ] All tasks completed in order using test-first changes.
- [ ] The active cover is responsive, capped at `340dp`, and constrained by available height.
- [ ] Only the immediate previous and next covers appear as 10–15% layered previews.
- [ ] Snapping, hard stops, metadata synchronization, favorites, and tap behavior work as designed.
- [ ] One-album, two-album, endpoint, effects-off, and compact portrait cases are covered.
- [ ] JVM unit tests, screenshot validation, and Android test assembly pass.
- [ ] Focused connected UI tests pass when a device is available.
- [ ] No unplanned files are modified and existing user-owned worktree changes remain untouched.
