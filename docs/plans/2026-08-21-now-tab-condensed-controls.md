# NOW Tab Condensed Controls Implementation Plan

**Date:** 2026-08-21  
**Design doc:** `docs/specs/2026-08-21-now-tab-condensed-controls-design.md`  
**Status:** Ready for review

## Overview

Condense the NOW tab by moving the play count into the album marquee and relocating the current-track `FAV` action to the far right of the `SHF`/`RPT` row. Give each repeat mode a distinct label and accessibility description while preserving the existing playback callbacks, state model, repeat cycle, visualizer, seek behavior, and queue behavior. The implementation must leave the unrelated uncommitted queue-shuffle changes in the playback layer untouched.

## Tasks

### Task 1: Add repeat-mode UI coverage (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingControlsTest.kt`

**Test first:**

Create `NowPlayingControlsTest` with a Compose rule and a test that renders `NowPlayingScreen` from mutable `PlaybackUiState`. Use Media3's `Player.REPEAT_MODE_OFF`, `Player.REPEAT_MODE_ALL`, and `Player.REPEAT_MODE_ONE` constants rather than numeric literals.

```kotlin
@Test fun repeatButtonShowsTheCurrentModeAndKeepsItsCallback() {
    var state by mutableStateOf(PlaybackUiState(repeatMode = Player.REPEAT_MODE_OFF))
    var repeatClicks = 0
    compose.setContent {
        NocturneLTheme {
            NowPlayingScreen(
                state = state,
                albumArtwork = null,
                effectsEnabled = false,
                onPrevious = {},
                onToggle = {},
                onNext = {},
                onShuffle = {},
                onRepeat = { repeatClicks++ },
                onSeek = {},
            )
        }
    }

    compose.onNodeWithText("[ RPT ]").assertIsDisplayed()
    compose.onNodeWithContentDescription("Repeat off").performClick()
    assertEquals(1, repeatClicks)

    compose.runOnIdle { state = state.copy(repeatMode = Player.REPEAT_MODE_ALL) }
    compose.onNodeWithText("[ RPT:A ]").assertIsDisplayed()
    compose.onNodeWithContentDescription("Repeat all").assertIsDisplayed()

    compose.runOnIdle { state = state.copy(repeatMode = Player.REPEAT_MODE_ONE) }
    compose.onNodeWithText("[ RPT:1 ]").assertIsDisplayed()
    compose.onNodeWithContentDescription("Repeat one").assertIsDisplayed()
}
```

Also assert after each state change that the labels and descriptions belonging to the other two modes do not exist. Write and compile this test before changing production code; compilation should succeed, while the ALL and ONE label/description assertions must fail against the current fixed `RPT`/`Repeat` button.

**Implementation:**

None in this task. Do not alter playback cycling or state.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device or emulator available, run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.NowPlayingControlsTest
```

Confirm the new test fails only on the expected dynamic repeat-label contract. If no device is attached, record runtime red/green execution as pending without weakening the assertions.

### Task 2: Render the repeat mode explicitly (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingControlsTest.kt`

**Test first:**

Use the failing repeat-mode test from Task 1. Keep its callback assertion and all three mutually exclusive label/description checks intact.

**Implementation:**

- Import `androidx.media3.common.Player` in `NowPlayingScreen.kt`.
- Derive the repeat glyph directly from `state.repeatMode`:
  - `Player.REPEAT_MODE_ALL` → `RPT:A`;
  - `Player.REPEAT_MODE_ONE` → `RPT:1`;
  - every other value → `RPT`.
- Derive the matching content description as `Repeat all`, `Repeat one`, or `Repeat off`.
- Pass the derived glyph and description to the existing repeat `BracketIconButton`.
- Set `selected` to `state.repeatMode != Player.REPEAT_MODE_OFF` so ALL and ONE retain active styling while OFF remains inactive.
- Keep `onRepeat` unchanged and do not modify `PlaybackConnection.cycleRepeat`, `PlaybackUiState`, the service, or persistence.

Prefer two small local `when` expressions inside `NowPlayingScreen`; do not add a new public type or shared abstraction for three presentation-only mappings.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. On a connected device/emulator, rerun `NowPlayingControlsTest`; all repeat labels, descriptions, exclusivity checks, and the callback assertion pass.

### Task 3: Move the play count into album metadata (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingControlsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`

**Test first:**

Add a test that renders a current track with `album = "Red Horizon"`, `currentPath = "red/01.flac"`, and `currentTrackPlayCount = 7`:

```kotlin
@Test fun currentTrackPlayCountIsPartOfTheAlbumMetadata() {
    compose.setContent {
        NocturneLTheme {
            NowPlayingScreen(
                state = PlaybackUiState(
                    title = "Carrier",
                    artist = "Signal One",
                    album = "Red Horizon",
                    currentPath = "red/01.flac",
                ),
                albumArtwork = null,
                effectsEnabled = false,
                onPrevious = {},
                onToggle = {},
                onNext = {},
                onShuffle = {},
                onRepeat = {},
                onSeek = {},
                currentTrackPlayCount = 7,
            )
        }
    }

    compose.onNodeWithText("Red Horizon · 7 PLAY(S)").assertIsDisplayed()
    compose.onNodeWithText("Red Horizon").assertDoesNotExist()
    compose.onNodeWithText("7 PLAY(S)").assertDoesNotExist()
}
```

Run the focused connected test before implementation. It must fail because the album and play count are currently separate nodes.

**Implementation:**

- Change the album `TerminalMarquee` text so a current track (`state.currentPath != null`) renders `${state.album.orEmpty()} · $currentTrackPlayCount PLAY(S)`.
- Preserve `state.album.orEmpty()` exactly when no current track exists; do not show a separator or play count in the empty state.
- Remove the standalone play-count `Text` from the conditional metadata row.
- Temporarily leave the existing conditional `FavoriteToggle` in place by itself; Task 4 will relocate it and eliminate that row completely.
- Keep title and artist marquees, error notice placement, and all playback inputs unchanged.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device/emulator, rerun `NowPlayingControlsTest`; the combined metadata test passes and the repeat test remains green.

### Task 4: Group secondary controls and right-align favorite (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingControlsTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`

**Test first:**

Add a test that renders the screen inside a 412dp-wide `Box`, supplies a current track through mutable state, and counts shuffle, repeat, and favorite callbacks. Use text nodes for like-for-like layout bounds and the favorite content-description node for its callback.

```kotlin
val shuffleBounds = compose.onNodeWithText("[ SHF ]").fetchSemanticsNode().boundsInRoot
val repeatBounds = compose.onNodeWithText("[ RPT:A ]").fetchSemanticsNode().boundsInRoot
val favorite = compose.onNodeWithContentDescription("Add Carrier to favorites")
val favoriteBounds = compose.onNodeWithText("[ FAV ]").fetchSemanticsNode().boundsInRoot

assertTrue(kotlin.math.abs(shuffleBounds.top - repeatBounds.top) <= 1f)
assertTrue(kotlin.math.abs(repeatBounds.top - favoriteBounds.top) <= 1f)
assertTrue(shuffleBounds.left < repeatBounds.left)
assertTrue(repeatBounds.right < favoriteBounds.left)
```

Click all three controls and assert each callback fires exactly once. Then use `compose.runOnIdle` to copy the mutable state with `currentPath = null`; assert both `Add Carrier to favorites` and `Remove Carrier from favorites` are absent while `SHF` and `RPT:A` remain present. Run the focused test before implementation; the current `FAV` position must fail the same-row bounds assertion.

**Implementation:**

- Delete the now-favorite-only conditional row above the error/seek area.
- Give the secondary-controls row `Modifier.fillMaxWidth()` and `horizontalArrangement = Arrangement.SpaceBetween`.
- Inside it, add a natural-width nested `Row` containing `SHF` followed by the dynamic repeat button, preserving their order and callbacks.
- As the outer row's second child, render `FavoriteToggle(state.title ?: "current track", currentTrackFavorite, onToggleCurrentFavorite)` only when `state.currentPath != null`.
- Do not add a placeholder when no favorite button is present. Do not change shared button dimensions, touch targets, colors, or `FavoriteToggle`.

This removes one full metadata/action row, which moves the error/seek area, time row, transport controls, secondary controls, and queue summary upward without hard-coded offsets.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. On a connected device/emulator, rerun `NowPlayingControlsTest`; metadata, repeat, alignment, empty-state visibility, and callback assertions all pass.

### Task 5: Make the NOW screenshot exercise the final states (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`

**Test first:**

After Tasks 2–4, run `.\gradlew.bat validateDebugScreenshotTest`. Confirm the existing `NowPlayingPreview` reference differs because the metadata and control rows have condensed. Investigate any unrelated mismatch before updating references.

**Implementation:**

- Import `androidx.media3.common.Player` in the screenshot source if it is not already present.
- In `NowPlayingPreview`, set `repeatMode = Player.REPEAT_MODE_ALL` in `PlaybackUiState` so the reference exercises `RPT:A`.
- Pass `currentTrackFavorite = true` and `currentTrackPlayCount = 7` to `NowPlayingScreen` using named arguments.
- Keep the preview name, 412x915 dimensions, artwork, track metadata, playback state, shuffle state, queue entry, and callbacks otherwise unchanged.
- Do not alter any other preview or screenshot reference in this task.

**Verify:** Run `.\gradlew.bat compileDebugScreenshotTestKotlin`. The screenshot preview source compiles.

### Task 6: Update and inspect the NOW screenshot reference (2–5 min)

**Files:** `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/NowPlayingPreview_Now playing_2ccda716_0.png`

**Test first:**

Run `.\gradlew.bat validateDebugScreenshotTest` and confirm the NOW reference is the only expected failure caused by this feature. Do not accept changes originating from the unrelated dirty playback files.

**Implementation:**

Run `.\gradlew.bat updateDebugScreenshotTest`, inspect every changed image, and retain only the NOW reference update. Confirm visually that:

- the album line reads `RED HORIZON · 7 PLAY(S)`;
- the standalone count row is gone;
- `SHF` and `RPT:A` are grouped on the left;
- `FAV` is at the far right of the same row;
- the active controls use the established selected color;
- the scrubber, times, controls, queue summary, and `QUEUE` button have shifted upward;
- no label is clipped or wrapped at 412dp.

If the screenshot tool generates a replacement filename, identify the exact old and new NOW paths before removing only the superseded NOW reference; never rename generated images manually.

**Verify:** Run `.\gradlew.bat validateDebugScreenshotTest`. All references pass with no unrelated image changes.

### Task 7: Run final regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–6

**Test first:**

Review the feature-scoped diff:

```powershell
git diff -- app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingControlsTest.kt app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt
```

Confirm there are no changes to playback services, `PlaybackConnection`, state persistence, queue behavior, shared buttons, other screens, or unrelated screenshot references.

**Implementation:**

Fix only failures caused by the approved NOW-tab presentation change. Preserve the existing uncommitted `NocturneLPlaybackService.kt`, `PlaybackConnection.kt`, `QueueEditingWiringTest.kt`, `QueueShufflePolicy.kt`, and `QueueShufflePolicyTest.kt` work exactly as found. Do not alter repeat cycling, favorite persistence, play-count persistence, navigation, visualizers, dependencies, or queue editing.

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
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.NowPlayingControlsTest
```

Report connected tests as pending when no device is available. Confirm the final status contains only this approved NOW-tab work, its design/plan/reference, and the pre-existing queue-shuffle changes.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] New Compose behavior was covered by failing tests before production changes, or device-dependent red execution was explicitly reported pending.
- [ ] A loaded track's album metadata renders as `ALBUM · N PLAY(S)` with no standalone count row.
- [ ] An empty NOW state shows neither a play count nor `FAV`.
- [ ] `SHF` and the dynamic repeat button remain grouped on the left.
- [ ] `FAV` is at the far right of the same secondary-controls row and retains its callback and selected styling.
- [ ] Repeat renders `RPT`, `RPT:A`, and `RPT:1` for OFF, ALL, and ONE respectively.
- [ ] Repeat accessibility descriptions distinguish off, all, and one, and the callback/cycle behavior is unchanged.
- [ ] The scrubber, times, playback controls, and queue section move upward by one removed row.
- [ ] The updated NOW reference passes screenshot validation and no unrelated reference changes are retained.
- [ ] Unit tests, Android-test assembly, screenshot validation, lint, and debug assembly pass.
- [ ] Connected NOW controls tests pass when a device is available, or are explicitly reported pending.
- [ ] `git diff --check` passes and no unrelated files are modified.
- [ ] Pre-existing queue-shuffle changes remain intact.
