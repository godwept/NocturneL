# Visualizer Sync Overlay Implementation Plan

**Date:** 2026-08-20  
**Design doc:** `docs/specs/2026-08-20-visualizer-sync-overlay-design.md`  
**Status:** Ready for review

## Overview

Keep the Now Playing display square fixed while moving visualizer synchronization controls into an accessible overlay: minus at top-left, plus at top-right, a tappable reset/value label at top-center for three seconds, and the existing transient mode label at bottom-right. Expand the shared persisted offset to `-2000..+2000 ms` without changing its `25 ms` step, playback wiring, audio timing, visualizer rendering, or PCM-buffer capacity. The work proceeds test-first through offset policy, persistence, overlay geometry, transient timing, touch behavior, screenshots, and device verification.

## Tasks

### Task 1: Expand the offset policy (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/VisualizerSyncOffsetTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/VisualizerSyncOffset.kt`

**Test first:**

Update `clampsAdjustsAndFormatsOffsets` before changing production constants. Preserve the default and step assertions, but require:

```kotlin
assertEquals(-2_000, VisualizerSyncOffset.MIN_MS)
assertEquals(2_000, VisualizerSyncOffset.MAX_MS)
assertEquals(-2_000, VisualizerSyncOffset.clamp(-9_999))
assertEquals(2_000, VisualizerSyncOffset.clamp(9_999))
assertEquals(-2_000, VisualizerSyncOffset.decrease(-2_000))
assertEquals(2_000, VisualizerSyncOffset.increase(2_000))
```

Keep the existing `0`, `+150 ms`, and `-25 ms` formatting assertions. Run the focused test and confirm it fails only because the current constants remain `-500` and `1_000`.

**Implementation:**

In `VisualizerSyncOffset.kt`, change only `MIN_MS` to `-2_000` and `MAX_MS` to `2_000`. Leave `DEFAULT_MS`, `STEP_MS`, clamping, adjustment, and label formatting unchanged.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.visualizer.VisualizerSyncOffsetTest"
```

The focused test passes.

### Task 2: Lock in persistence and view-model behavior at the new limits (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepositoryTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsViewModelTest.kt`

**Test first:**

In `TerminalPreferencesRepositoryTest.visualizerSyncOffsetDefaultsPersistsAndClamps`:

- Persist `-2_000` and recreate the repository; assert `-2_000` is restored.
- Persist `2_000` and recreate the repository; assert `2_000` is restored.
- Retain out-of-range `-9_999` and `9_999` cases and assert they clamp through `VisualizerSyncOffset.MIN_MS` and `MAX_MS`.

In `SettingsViewModelTest.adjustsResetsClampsAndRestoresVisualizerSyncOffset`, replace the old fixed repeat counts with counts derived from the policy so the test reaches and attempts to cross both new limits:

```kotlin
repeat(VisualizerSyncOffset.MAX_MS / VisualizerSyncOffset.STEP_MS + 1) {
    viewModel.increaseVisualizerSyncOffset()
}
assertEquals(VisualizerSyncOffset.MAX_MS, viewModel.state.value.visualizerSyncOffsetMs)

repeat((VisualizerSyncOffset.MAX_MS - VisualizerSyncOffset.MIN_MS) /
    VisualizerSyncOffset.STEP_MS + 1) {
    viewModel.decreaseVisualizerSyncOffset()
}
assertEquals(VisualizerSyncOffset.MIN_MS, viewModel.state.value.visualizerSyncOffsetMs)
```

**Implementation:**

No repository or view-model change should be necessary because both already delegate to `VisualizerSyncOffset`. If a focused test exposes a hard-coded old boundary, replace only that boundary with the existing policy constant; do not add migration or new state.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device or emulator available, run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.settings.TerminalPreferencesRepositoryTest
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.settings.SettingsViewModelTest
```

Both focused classes pass. If no device is available, record their execution as pending without weakening the assertions.

### Task 3: Establish fixed-square overlay geometry (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`

**Test first:**

Replace `syncControlsSitAboveAnUnchangedSquareVisualizer` with `syncControlsOverlayAnUnmovedSquareVisualizer`. Render at `Modifier.width(240.dp)`, capture artwork bounds, enter radar, then assert every square edge remains unchanged:

```kotlin
assertEquals(artBounds.left, visualizerBounds.left, 0.5f)
assertEquals(artBounds.top, visualizerBounds.top, 0.5f)
assertEquals(artBounds.right, visualizerBounds.right, 0.5f)
assertEquals(artBounds.bottom, visualizerBounds.bottom, 0.5f)
assertEquals(visualizerBounds.width, visualizerBounds.height, 0.5f)
```

Fetch `visualizer-sync-decrease`, `visualizer-sync-reset`, and `visualizer-sync-increase` bounds. Assert all three are inside the visualizer bounds, decrease occupies the top-left region, reset/value occupies the top-center region, and increase occupies the top-right region. Compare centers against the square center and compare each control's top to the square's top with a `0.5f` tolerance. Add a `visualizer-mode-label` tag expectation and assert its right and bottom edges sit inside the square's bottom-right region.

Retain assertions that no sync overlay exists over album art, controls remain present in bands, and returning to artwork removes them. Run the focused connected test before production changes; the old above-square assertion and missing mode-label tag must fail.

**Implementation:**

Restructure `VisualizerDeck.kt` as one fixed `Box` using the existing `fillMaxWidth().aspectRatio(1f)` display modifier; remove the conditional `Column` row entirely. Keep artwork or `TerminalVisualizerScene` as the full-size bottom layer, keep the mode-cycling click target over the scene, and compose sync controls afterward only when `visualizerActive` is true.

Change `VisualizerSyncControls` from a horizontal `Row` into a full-size overlay `Box` tagged `visualizer-sync-controls`:

- Align `visualizer-sync-decrease` to `Alignment.TopStart`.
- Align the current reset/value button to `Alignment.TopCenter`.
- Align `visualizer-sync-increase` to `Alignment.TopEnd`.
- Continue clamping the displayed value and deriving enabled states from `VisualizerSyncOffset.MIN_MS` and `MAX_MS`.
- Preserve the existing button components, content descriptions, reset callback, and minimum touch targets.

Move the existing transient mode `Text` from `Alignment.TopEnd` to `Alignment.BottomEnd` and add `Modifier.testTag("visualizer-mode-label")`. Keep its current text and animation timing unchanged.

At this slice, the sync reset/value may remain visible for the full time a visualizer is active; Task 4 adds the approved transient behavior.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device available, rerun `VisualizerDeckTest` and confirm the square-edge and overlay-position assertions pass.

### Task 4: Add the three-second entry timer (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`

**Test first:**

Add `syncLabelAppearsForThreeSecondsOnEachVisualizerSelection`. Disable automatic clock advancement before entering radar:

```kotlin
compose.mainClock.autoAdvance = false
compose.onNodeWithTag("visualizer-art").performClick()
compose.onNodeWithTag("visualizer-sync-reset").assertExists()
compose.mainClock.advanceTimeBy(2_599)
compose.onNodeWithTag("visualizer-sync-reset").assertExists()
compose.mainClock.advanceTimeBy(402)
compose.onNodeWithTag("visualizer-sync-reset").assertDoesNotExist()
```

Then select bands, assert the reset/value label appears again, cycle to art, and assert the entire sync overlay disappears immediately. Use `advanceTimeBy` plus `waitForIdle`/`runOnIdle` as needed; do not use wall-clock sleeps. The test must fail while Task 3 leaves the value permanently visible.

**Implementation:**

Add sync-label state independent of the existing mode-label state: a visibility boolean and its own `Animatable<Float>`. Key a `LaunchedEffect` to the selected mode and a sync-interaction generation counter. When the selected mode is radar or bands:

1. Set the label visible and snap alpha to `1f`.
2. Hold for `2_600 ms`.
3. Fade to `0f` with the existing `tween(400)` convention.
4. Remove the label after the fade, making total visibility three seconds.

When mode becomes artwork, immediately hide the label and snap its alpha to zero. Pass visibility and alpha into `VisualizerSyncControls`; compose the reset/value `BracketButton` only while visible, applying the animated alpha. Keep minus and plus permanently visible in active visualizer modes.

Selecting radar or bands—including radar-to-bands—must restart the effect. Do not reuse the mode label's visibility or alpha state.

**Verify:** Assemble Android tests and run the focused `VisualizerDeckTest` on a device/emulator. The label timing test passes without sleeping in real time.

### Task 5: Reveal on interaction and protect mode cycling (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`

**Test first:**

Expand `syncControlsAdjustWithoutCyclingAndRespectLimits` so its callbacks update `offsetMs` through `VisualizerSyncOffset.decrease`, `increase`, and the default value. Verify:

- After the entry label expires, tapping minus reveals the new signed value and radar remains selected.
- Advancing 2 seconds, tapping plus, and advancing another 1.5 seconds leaves the label visible, proving the timer restarted after the latest interaction.
- Tapping the visible reset/value invokes reset, displays `VIS SYNC 0 MS`, and leaves the mode unchanged.
- At `-2_000 ms`, decrease is disabled; at `+2_000 ms`, increase is disabled.
- Tapping either disabled corner hit area does not cycle radar to bands.

Add or retain assertions for the exact accessibility descriptions:

```text
Decrease visualizer sync offset
Increase visualizer sync offset
Reset visualizer sync offset, currently +150 ms
```

Run the test before implementation; reveal/restart and disabled-hit behavior must fail.

**Implementation:**

Wrap each active sync action inside `VisualizerDeck` so it increments the sync-interaction generation and then invokes the existing callback. The reset wrapper does the same. This restarts Task 4's effect even if a caller's callback does not change the offset.

Ensure each corner owns and consumes its full 48dp touch target, including when its action is disabled, so the underlying mode-cycle target never receives that tap. Preserve disabled semantics and styling; use a local pointer-consuming wrapper around a disabled corner if necessary rather than changing the shared `BracketButton` API. Empty portions of the square outside active controls continue to cycle modes.

Do not alter the callback signatures, persisted source of truth, display-mode order, or shared button implementation.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest` and the focused connected `VisualizerDeckTest`. All interaction, timer-restart, semantics, limit, and mode-preservation assertions pass.

### Task 6: Update the Now Playing integration contract (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt`

**Test first:**

Update `exposesSharedSyncControlsOnlyForVisualizerModes` to reflect the overlay contract while retaining its real `NowPlayingScreen` wiring:

- Capture `visualizer-art` bounds, enter radar, and assert `visualizer-deck` has the same left, top, right, and bottom edges.
- Assert minus, plus, and `VIS SYNC +75 MS` are displayed inside radar.
- Invoke decrease, increase, and reset, confirming one callback each and that radar remains selected.
- Cycle to bands and confirm the corner controls remain present.
- Cycle to artwork and confirm `visualizer-sync-controls` is absent.

Use existing tags and a `0.5f` bounds tolerance. No production changes are expected; if the integration test fails, correct only `VisualizerDeck` behavior from Tasks 3–5.

**Implementation:**

None expected. Keep `NowPlayingScreen`, `NocturneLApp`, settings callbacks, playback connection, and analyzer wiring unchanged.

**Verify:** Run `.\gradlew.bat assembleDebugAndroidTest`. With a device available, run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.NowPlayingVisualizerTest
```

The focused class passes.

### Task 7: Convert the deterministic preview to the overlay (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`

**Test first:**

After Tasks 3–5, run `.\gradlew.bat validateDebugScreenshotTest` and confirm the existing `VisualizerSyncControlsPreview` reference fails because it still describes the rejected separate row.

**Implementation:**

Keep the preview name and its deterministic radar fixture, but replace the `Column` with a 392dp square `Box`. Render `TerminalVisualizerScene` with `Modifier.fillMaxSize()`, then render `VisualizerSyncControls` over it with `Modifier.fillMaxSize()`, the `150 ms` offset, no-op callbacks, and explicit fully-visible label inputs (`labelVisible = true`, `labelAlpha = 1f`). Change the preview height from `460` to `412` so the reference frames the fixed square rather than reserving former row space. Remove imports made unused by this preview only; preserve unrelated previews exactly.

**Verify:** Run `.\gradlew.bat compileDebugScreenshotTestKotlin` if available, otherwise run `.\gradlew.bat updateDebugScreenshotTest --dry-run` to confirm the preview source compiles and the screenshot task resolves.

### Task 8: Regenerate and inspect the affected screenshot reference (2–5 min)

**Files:** `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/VisualizerSyncControlsPreview_Visualizer sync controls_1e8b1f2f_0.png`

**Test first:**

Run `.\gradlew.bat validateDebugScreenshotTest`; it must report only the expected sync-preview mismatch or generated-name replacement caused by Task 7.

**Implementation:**

Run `.\gradlew.bat updateDebugScreenshotTest`. Inspect every changed reference and retain only the VIS SYNC preview change. Confirm visually that:

- Radar remains a full, unclipped square.
- Minus is top-left and plus is top-right.
- `VIS SYNC +150 MS` is legible at top-center.
- The controls do not create space above or below the square.
- Terminal-green styling and 48dp touch-target spacing remain consistent.

If the preview dimension changes the generated filename, identify the exact new file under the same reference directory and remove only the superseded sync-preview reference. Do not rename generated images manually or accept changes to `NowPlayingPreview` or unrelated references.

**Verify:** Run `.\gradlew.bat validateDebugScreenshotTest`. All screenshot references pass.

### Task 9: Update the Pixel 7 verification contract (2–5 min)

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Review the existing visualizer-sync checklist block and identify its now-stale `-500 ms` and `+1000 ms` assertion plus the missing fixed-position and transient-label checks.

**Implementation:**

Update only the visualizer-sync checklist items to require:

- Album art, radar, and bands retain identical square bounds with no vertical jump.
- Minus and plus occupy the visualizer's top corners and never change modes.
- The current sync label appears on visualizer entry, resets its three-second timeout after adjustments, fades away, and resets to `0 ms` when tapped.
- The mode label appears at bottom-right.
- Minus disables at `-2000 ms` and plus disables at `+2000 ms`.
- Calibration in the target vehicle confirms the expanded range is sufficient and remains shared by both visualizers.

Preserve all unrelated release checks.

**Verify:** Run `rg -n "visualizer sync|-2000|\+2000|three-second|bottom-right" docs/testing/pixel-7-release-checklist.md` and inspect the resulting block for consistency and absence of the old limits.

### Task 10: Run final regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–9

**Test first:**

Review the scoped diff before cleanup:

```powershell
git diff -- app/src/main/java/ca/stewark/nocturnel/visualizer/VisualizerSyncOffset.kt app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt app/src/test/java/ca/stewark/nocturnel/visualizer/VisualizerSyncOffsetTest.kt app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepositoryTest.kt app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsViewModelTest.kt app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt docs/testing/pixel-7-release-checklist.md
```

Confirm the diff contains no playback timing, PCM buffer, visualizer renderer, mode ordering, shared component, or unrelated preview changes.

**Implementation:**

Fix only failures introduced by this approved feature. Do not expand scope, change the `25 ms` step, increase buffer capacity, or alter audio playback.

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
.\gradlew.bat connectedDebugAndroidTest
```

On the Pixel 7, perform the updated visualizer-sync checklist with the target vehicle. Explicitly report connected/device checks as pending if the required hardware is unavailable.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] New behavior was covered by failing tests before each production change.
- [ ] The shared offset clamps, persists, adjusts, and resets from `-2000 ms` through `+2000 ms` in `25 ms` steps.
- [ ] Album art, radar, and bands have identical square position and dimensions.
- [ ] Minus and plus occupy the active visualizer's top corners and are absent over album art.
- [ ] The sync reset/value label appears on visualizer entry and after interaction, then fully fades within three seconds of the latest trigger.
- [ ] Tapping the value resets to `0 ms`; all sync-control taps leave the selected display mode unchanged.
- [ ] Disabled corner controls consume taps without changing modes and expose correct disabled accessibility state.
- [ ] The transient mode label appears at bottom-right and remains independent from sync-label timing.
- [ ] Persistence, playback wiring, audio timing, renderers, mode ordering, step size, and PCM-buffer capacity remain unchanged.
- [ ] The deterministic sync screenshot is inspected and no unrelated reference changes are accepted.
- [ ] The Pixel 7 checklist reflects fixed geometry, transient feedback, new limits, and vehicle calibration.
- [ ] Unit tests, Android-test assembly, screenshot validation, lint, and debug assembly pass.
- [ ] Connected tests and the vehicle check pass when hardware is available, or are explicitly reported pending.
- [ ] `git diff --check` passes and no unrelated files are modified.
