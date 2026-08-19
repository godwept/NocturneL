# Visualizer Sync Layout Implementation Plan

**Date:** 2026-08-19  
**Design doc:** `docs/specs/2026-08-19-visualizer-sync-layout-design.md`  
**Status:** Ready for review

## Overview

Remove the `[ NOW PLAYING ]` caption and move the existing VIS SYNC controls out of the visualizer overlay into a centered, conditional row above the square. `VisualizerDeck` will continue to own display-mode state and sync visibility, the square will retain its current width and `1:1` dimensions, album-art mode will reserve no sync-row space, and all existing sync callbacks, limits, labels, accessibility semantics, and mode-cycling behavior will remain unchanged. The implementation must preserve the unrelated in-progress ring-removal edits already present in the visualizer tests and screenshot preview file.

## Tasks

### Task 1: Establish the failing layout and caption contract (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt`

**Test first:**

In `VisualizerDeckTest`, preserve the current three-mode expectations and change test deck constraints from `Modifier.size(240.dp)` to `Modifier.width(240.dp)` so the component may become taller than its unchanged square. Add a focused test that:

1. Renders the deck at a fixed width and captures the `visualizer-art` bounds.
2. Confirms those artwork bounds are square.
3. Taps artwork to select radar.
4. Captures the `visualizer-deck` and `visualizer-sync-controls` bounds.
5. Confirms the radar square has the same width and height as the artwork square.
6. Confirms `syncBounds.bottom <= visualizerBounds.top`, proving VIS SYNC is in separate space above the square.

Use Compose semantics bounds with a small floating-point tolerance:

```kotlin
val artBounds = compose.onNodeWithTag("visualizer-art").fetchSemanticsNode().boundsInRoot
assertEquals(artBounds.width, artBounds.height, 0.5f)

compose.onNodeWithTag("visualizer-art").performClick()

val visualizerBounds = compose.onNodeWithTag("visualizer-deck").fetchSemanticsNode().boundsInRoot
val syncBounds = compose.onNodeWithTag("visualizer-sync-controls").fetchSemanticsNode().boundsInRoot
assertEquals(artBounds.width, visualizerBounds.width, 0.5f)
assertEquals(artBounds.height, visualizerBounds.height, 0.5f)
assertTrue(syncBounds.bottom <= visualizerBounds.top)
```

In `NowPlayingVisualizerTest`, add `compose.onNodeWithText("[ NOW PLAYING ]").assertDoesNotExist()` to the initial album-art state. Keep the existing checks that sync controls are absent on artwork and present and functional in visualizer modes.

Run the tests before production changes. The bounds test must fail because VIS SYNC currently overlaps the square, and the caption assertion must fail because the frame still supplies `NOW PLAYING`.

**Implementation:**

None in this task. This is the required red phase.

**Verify:** With an emulator or device attached, run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.visualizer.VisualizerDeckTest
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.NowPlayingVisualizerTest
```

Both commands fail only on the newly introduced layout/caption expectations. If no device is attached, run `.\gradlew.bat assembleDebugAndroidTest` to validate compilation and record the red execution as pending rather than weakening the assertions.

### Task 2: Move VIS SYNC above the square (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`

**Test first:**

Use the failing `VisualizerDeckTest` case from Task 1. Retain its existing behavioral assertions that:

- Sync controls do not exist in album-art mode.
- Reset, decrement, and increment clicks leave radar selected.
- Controls remain visible in spectrum mode.
- Returning to album art removes the controls.
- Minimum and maximum offsets disable only the corresponding control.

**Implementation:**

In `VisualizerDeck.kt`:

- Add the Compose layout imports for `Column`, `aspectRatio`, and `fillMaxWidth`.
- Make the top-level container a `Column(modifier)`.
- When `visualizerActive` is true, compose `VisualizerSyncControls` first and center it horizontally with `Modifier.align(Alignment.CenterHorizontally)`.
- Follow it with the existing tappable display `Box`, applying `Modifier.fillMaxWidth().aspectRatio(1f)` to that box before its existing test tag, state description, and click behavior.
- Keep `visualizer-art`/`visualizer-deck`, album artwork, `TerminalVisualizerScene`, and the transient mode label inside this square box.
- Remove the old bottom-center `VisualizerSyncControls` overlay from inside the box.
- Do not add placeholder height in album-art mode or change control labels, callbacks, enabled rules, semantics, animation timing, or visualizer-active effects.

The resulting shape should be equivalent to:

```kotlin
Column(modifier) {
    if (visualizerActive) {
        VisualizerSyncControls(
            // existing values and callbacks
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            // existing tag, semantics, and clickable chain
    ) {
        // existing art/visualizer and transient label only
    }
}
```

Carefully merge this structure with the current `VisualizerDeckTest` content; do not restore removed ring assertions or overwrite other pending visualizer edits.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device available, rerun the focused `VisualizerDeckTest`; all size, position, mode-cycle, callback, and limit assertions pass.

### Task 3: Remove the caption and preserve the display width (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt`

**Test first:**

Use the failing caption assertion from Task 1. Keep the test rendering a real `NowPlayingScreen`, rather than testing `AsciiFrame` independently, so it proves the user-facing screen no longer supplies the title.

**Implementation:**

- Change `AsciiFrame("NOW PLAYING") { ... }` to `AsciiFrame { ... }`; do not remove the frame itself.
- Change the `VisualizerDeck` modifier from `Modifier.fillMaxWidth().aspectRatio(1f)` to `Modifier.fillMaxWidth()` because the deck now owns the square aspect ratio internally.
- Remove the now-unused `aspectRatio` import from `NowPlayingScreen.kt`.
- Leave metadata, favorite state, seek controls, playback buttons, queue summary, and all visualizer inputs/callbacks unchanged.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device available, run the focused `NowPlayingVisualizerTest`; `[ NOW PLAYING ]` is absent, album art begins at the top of the retained frame, and the existing sync callback test passes.

### Task 4: Update deterministic layout previews and references (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/NowPlayingPreview_Now playing_2ccda716_0.png`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/VisualizerSyncControlsPreview_Visualizer sync controls_72a3cb8f_0.png`

**Test first:**

Run `.\gradlew.bat validateDebugScreenshotTest` after Tasks 2 and 3. It must report the expected Now Playing difference caused by removing the caption. Update the `VisualizerSyncControlsPreview` source before regenerating references so it represents the approved above-square layout instead of the rejected bottom overlay.

**Implementation:**

- Keep the Now Playing preview inputs unchanged; its reference should change only because the caption is gone and the unchanged square moves upward within the retained frame.
- Change `VisualizerSyncControlsPreview` from a `Box` with bottom-aligned overlay controls to a `Column`: center `VisualizerSyncControls` first, then render the existing radar scene in a `Modifier.fillMaxWidth().aspectRatio(1f)` square.
- Increase that preview's height enough to include the full 392dp square plus the existing 48dp control touch target without clipping; use `heightDp = 460` while retaining `widthDp = 412`.
- Preserve the preview's radar fixture, `150 ms` value, effects setting, and callbacks.
- Run `.\gradlew.bat updateDebugScreenshotTest`, inspect every changed or newly named reference, and retain only the Now Playing and VIS SYNC layout changes. Because this source file already has pending ring-removal edits, do not restore ring previews or accept unrelated reference changes.
- If the preview dimension change produces a new generated filename, remove only the superseded VIS SYNC reference after resolving and confirming its exact path under `app/src/screenshotTestDebug/reference`; do not manually rename generated images.

**Verify:** Run `.\gradlew.bat validateDebugScreenshotTest`. Inspect the 412dp references and confirm there is no `[ NOW PLAYING ]`, VIS SYNC is centered above rather than over radar, the square is not clipped or reduced, and no unrelated reference changed.

### Task 5: Run final regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–4

**Test first:**

Review the final diff before making any cleanup:

```powershell
git diff -- app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt
```

Confirm the layout work does not reintroduce ring code or alter unrelated pending changes.

**Implementation:**

Fix only failures caused by the approved VIS SYNC layout. Do not change synchronization math or persistence, visualizer renderers/geometry, display-mode ordering, offset limits, shared buttons, `AsciiFrame`, playback behavior, dependencies, or unrelated screenshot previews.

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

If an emulator or device is attached, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Confirm the final diff contains only this approved layout work, its design/plan/tests/references, and the pre-existing uncommitted ring-removal work. Explicitly report connected tests as pending if no device is available.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] New Compose assertions were written and observed failing before production changes, or their device-dependent red execution was explicitly reported pending.
- [ ] The outer Now Playing frame remains and `[ NOW PLAYING ]` is absent in every display mode.
- [ ] VIS SYNC is absent in album-art mode with no reserved gap.
- [ ] VIS SYNC is centered in separate space immediately above radar and spectrum modes.
- [ ] Album art, radar, and spectrum retain equal-width, equal-height square bounds.
- [ ] The square remains the only mode-cycle tap target; sync controls retain their reset/decrement/increment behavior and offset-limit states.
- [ ] Existing labels, accessibility descriptions, persistence, synchronization behavior, and visualizer-active behavior remain unchanged.
- [ ] Updated 412dp screenshots show no clipping or unrelated visual changes.
- [ ] Pre-existing ring-removal edits remain intact and are not reverted or duplicated.
- [ ] Unit tests, Android-test assembly, screenshot validation, lint, and debug assembly pass.
- [ ] Connected tests pass when a device is available, or are explicitly reported pending.
- [ ] `git diff --check` passes and no unrelated files are modified.
