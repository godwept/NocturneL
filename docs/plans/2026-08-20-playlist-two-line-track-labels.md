# Playlist Two-Line Track Labels Implementation Plan

**Date:** 2026-08-20  
**Design doc:** `docs/specs/2026-08-20-playlist-two-line-track-labels-design.md`  
**Status:** Ready for review

## Overview

Replace the Playlist Details screen's combined `ARTIST :: TRACK` strings with one shared two-line label used by both playlist entries and Add Track results. Artist and title will each receive one independently ellipsized line while all add, remove, queue, reorder, accessibility, and drag behavior remains unchanged. The implementation must preserve the unrelated uncommitted VIS SYNC work already present in the working tree and shared screenshot source.

## Tasks

### Task 1: Establish the shared label contract (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`

**Test first:**

Replace `playlistTrackTextUsesSingleEllipsizedLines` with a focused `playlistTrackLabelEllipsizesArtistAndTitleIndependently` test that renders the new internal component directly at a narrow fixed width:

```kotlin
val longArtist = "The Extremely Long Terminal Ensemble Beyond The Horizon"
val longTitle = "Carrier Across The Endless Terminal Horizon Repeating Forever"
compose.setContent {
    NocturneLTheme {
        PlaylistTrackLabel(
            artist = longArtist,
            title = longTitle,
            modifier = Modifier.width(160.dp),
        )
    }
}

val artistLayout = compose.onNodeWithText(longArtist).textLayoutResult()
val titleLayout = compose.onNodeWithText(longTitle).textLayoutResult()
assertEquals(1, artistLayout.lineCount)
assertEquals(1, titleLayout.lineCount)
assertTrue(artistLayout.hasVisualOverflow)
assertTrue(titleLayout.hasVisualOverflow)
```

Also call `assertIsDisplayed()` on both separate text nodes. Write this test before adding the composable; Android-test compilation must fail because `PlaylistTrackLabel` does not yet exist.

**Implementation:**

Add an `internal` composable beside `PlaylistTrackEntryRow` in `PlaylistDetailScreen.kt`:

```kotlin
@Composable
internal fun PlaylistTrackLabel(
    artist: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
```

Use the existing `Column`, `Text`, and `TextOverflow` imports. Do not add styling, fallback text, click behavior, semantics, or metadata.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device or emulator available, run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playlist.PlaylistDetailScreenTest
```

The focused label case passes. If no device is attached, record connected execution as pending without weakening the assertions.

### Task 2: Convert playlist entries test-first (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`

**Test first:**

In `playlistRowsAreCompactAndAvailableRowsKeepAddActions`, give the existing entry a unique artist such as `Playlist Artist`. Replace the combined-string assertion with:

```kotlin
compose.onNodeWithText("Playlist Artist").assertIsDisplayed()
compose.onNodeWithText("Carrier").assertIsDisplayed()
compose.onNodeWithText("Playlist Artist :: Carrier").assertDoesNotExist()
```

Keep the reorder, remove, queue, action-row alignment, and button assertions unchanged. Run the focused connected class before production changes; the separate artist/title assertions must fail against the combined `Text` node.

**Implementation:**

In `PlaylistTrackEntryRow`, replace the combined `Text("${row.artist} :: ${row.title}", ...)` with:

```kotlin
PlaylistTrackLabel(
    artist = row.artist,
    title = row.title,
    modifier = Modifier
        .weight(1f)
        .padding(horizontal = TerminalDimensions.xs),
)
```

Leave `Row` vertical centering, drag modifiers, drag handle, remove button, callbacks, keys, and accessibility semantics untouched.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. On a connected device/emulator, rerun `PlaylistDetailScreenTest`; the entry label and existing row-action assertions pass.

### Task 3: Convert Add Track results test-first (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt`

**Test first:**

In `playlistRowsAreCompactAndAvailableRowsKeepAddActions`, use one available track with unique values such as `Available Artist` and `Available Carrier`. Pass that track through the test state, open Add Track, and replace its combined-string assertion with:

```kotlin
compose.onNodeWithText("Available Artist").assertIsDisplayed()
compose.onNodeWithText("Available Carrier").assertIsDisplayed()
compose.onNodeWithText("Available Artist :: Available Carrier").assertDoesNotExist()
```

Keep the add-button click and exact `relativePath` callback assertion. Run the focused connected class before production changes; these separate text assertions must fail while the available result still uses one combined `Text`.

**Implementation:**

In the `adding` branch of `PlaylistDetailScreen`:

- Add `verticalAlignment = Alignment.CenterVertically` to the available-track `Row` so its add button remains centered beside two text lines.
- Replace the combined text with `PlaylistTrackLabel(track.artist, track.title, Modifier.weight(1f).padding(horizontal = TerminalDimensions.xs))`.
- Remove the old top-only padding from the text area.
- Preserve filtering, item keys, add-button content descriptions, and callbacks exactly.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. On a connected device/emulator, rerun `PlaylistDetailScreenTest`; the available-result label and add callback assertions pass.

### Task 4: Update affected regression assertions (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt`

**Test first:**

Search the test file for remaining `" :: "` expectations. Update `duplicateAndUnavailableRowsKeepIndependentActions` to assert the unique title `Second` is displayed rather than looking for `Artist :: Second`. Do not use `onNodeWithText("Artist")` there because both duplicate rows intentionally share that artist.

Retain every drag gesture, move result, remove callback, lifted-state assertion, custom accessibility move, action visibility check, and row-key scenario. No production changes should be required; any failure must be fixed only in the owning two-line layout without relaxing interaction coverage.

**Implementation:**

None expected. Do not modify drag distance calculations, reorder state, data models, or callbacks.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. If a device/emulator is attached, run the complete `PlaylistDetailScreenTest` and confirm all interaction regressions pass.

### Task 5: Make the Playlist Detail preview exercise a long artist (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`

**Test first:**

After Tasks 1–3, run `.\gradlew.bat validateDebugScreenshotTest`. It must report expected differences for `PlaylistDetailPreview` and `PlaylistDraggedRowPreview` because their entry rows are now two lines. Confirm the already-modified VIS SYNC preview still validates and do not regenerate unrelated references.

**Implementation:**

Add a screenshot-only constant beside `previewLongTrackTitle`:

```kotlin
private const val previewLongArtist = "The Extremely Long Terminal Ensemble Beyond The Horizon"
```

In `PlaylistDetailPreview`, change only the first preview track to copy both `title = previewLongTrackTitle` and `artist = previewLongArtist`. Keep the preview name, 412x915 dimensions, rows, callbacks, and all unrelated previews unchanged. `PlaylistDraggedRowPreview` already supplies a long title and requires no source change.

Carefully preserve the uncommitted `VisualizerSyncControlsPreview` edits in this same file.

**Verify:** Run `.\gradlew.bat compileDebugScreenshotTestKotlin`. The preview source compiles.

### Task 6: Regenerate and inspect only the playlist references (2–5 min)

**Files:** `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/PlaylistDetailPreview_Playlist detail_1da3cfda_0.png`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/PlaylistDraggedRowPreview_Playlist dragged row_bfeb1d47_0.png`

**Test first:**

Run `.\gradlew.bat validateDebugScreenshotTest` and confirm only the two expected playlist references fail. Investigate any additional mismatch before updating references.

**Implementation:**

Run `.\gradlew.bat updateDebugScreenshotTest`. Inspect every changed image and retain only the Playlist Detail and Playlist Dragged Row changes caused by the shared two-line label. Confirm:

- Artist is above title in both references.
- The deliberately long artist and long titles ellipsize independently.
- Titles remain visible.
- Drag, add, and remove buttons remain vertically centered and unclipped.
- Row spacing remains readable at the existing Pixel 7 width.

Do not accept changes to the VIS SYNC reference or any unrelated screenshot. If the screenshot tool generates replacement filenames, resolve the exact old and new playlist paths before removing only superseded playlist references; never rename generated images manually.

**Verify:** Run `.\gradlew.bat validateDebugScreenshotTest`. All screenshot references pass.

### Task 7: Run final regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–6

**Test first:**

Review the playlist-scoped diff before cleanup:

```powershell
git diff -- app/src/main/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreen.kt app/src/androidTest/java/ca/stewark/nocturnel/ui/playlist/PlaylistDetailScreenTest.kt app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt
```

Confirm the shared screenshot file retains the existing VIS SYNC overlay preview and that no unrelated application file changed for this feature.

**Implementation:**

Fix only failures caused by the approved playlist label change. Do not alter playlist state, filtering, persistence, action callbacks, drag/reorder behavior, other track-row components, typography, colors, or dependencies.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat validateDebugScreenshotTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
git diff --check
git status --short
```

If a device or emulator is attached, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playlist.PlaylistDetailScreenTest
```

Explicitly report connected tests as pending if no device is available. Confirm final status contains only this approved playlist work, its design/plan/references, and the pre-existing VIS SYNC changes.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] New Compose behavior was covered by failing tests before production changes, or device-dependent red execution was explicitly reported pending.
- [ ] Existing playlist entries show artist on the first line and title on the second.
- [ ] Add Track results use the same shared two-line component.
- [ ] Artist and title each occupy one independently ellipsized line.
- [ ] Long artists cannot obscure or consume the title line.
- [ ] Add, remove, queue, reorder, accessibility, duplicate-row, and drag behavior remains unchanged.
- [ ] Action buttons remain vertically centered with their existing touch targets and content descriptions.
- [ ] Only the two intended playlist screenshot references change and pass validation after inspection.
- [ ] Pre-existing VIS SYNC source and reference changes remain intact.
- [ ] Unit tests, Android-test assembly, screenshot validation, lint, and debug assembly pass.
- [ ] Connected playlist tests pass when a device is available, or are explicitly reported pending.
- [ ] `git diff --check` passes and no unrelated files are modified.
