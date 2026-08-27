# Visualizer Sync Hold-to-Repeat Implementation Plan

**Date:** 2026-08-26
**Design doc:** `docs/specs/2026-08-26-visualizer-sync-hold-repeat-design.md`
**Status:** Ready for review

## Overview

Add accelerating touch hold behavior to the existing visualizer sync minus and plus buttons without changing their layout, application callbacks, persistence, offset policy, or accessible one-step click actions. Physical touch-down will adjust by 25 ms immediately, begin repeating after 400 ms at 100 ms intervals, and accelerate to 50 ms intervals after 1500 ms. The work proceeds test-first through immediate touch behavior, timing, cancellation, mutual exclusion, limit handling, documentation, and regression verification; offset presets remain deferred.

## Tasks

### Task 1: Lock in immediate touch and one-step click behavior (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`

**Test first:**

Add `syncTouchAdjustsOnDownWithoutASecondReleaseAdjustment`. Render `VisualizerDeck` in the same state-backed style as `syncControlsAdjustWithoutCyclingAndRespectLimits`, enter radar, disable automatic clock advancement, and press the increase node without releasing it:

```kotlin
compose.onNodeWithTag("visualizer-sync-increase").performTouchInput { down(center) }
advanceUi()
assertEquals(175, offsetMs)
assertEquals(1, increases)

compose.onNodeWithTag("visualizer-sync-increase").performTouchInput { up() }
advanceUi()
assertEquals(175, offsetMs)
assertEquals(1, increases)
compose.onNodeWithTag("visualizer-radar").assertIsDisplayed()
```

In the same test, invoke `performClick()` after release and assert it performs exactly one additional 25 ms adjustment. This preserves the semantic click path used by accessibility services and keyboard-like input. Run the focused connected test before implementation and confirm the current release-only clickable behavior fails the touch-down assertion.

**Implementation:**

Keep `BracketIconButton` as the visible and semantic button. In the private `SyncCornerButton`, add a same-size transparent overlay above the enabled button using `Modifier.matchParentSize().pointerInput(enabled)`. Have that overlay use `detectTapGestures` with:

- `onPress`: call the latest adjustment callback immediately, then await release or cancellation.
- `onTap = {}`: consume the completed physical tap without invoking the callback again.

Use `rememberUpdatedState(onClick)` so recomposition supplies the current callback without restarting an active pointer coroutine. Preserve the existing disabled pointer-consuming wrapper, button tag, enabled semantics, styling, and 48dp minimum touch target. Do not change `BracketButton`, `BracketIconButton`, `VisualizerDeck` callback signatures, or offset policy.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.visualizer.VisualizerDeckTest
```

The new immediate-touch test passes. If no device or emulator is attached, record connected execution as pending after the Android-test APK assembles.

### Task 2: Add delayed fixed-rate repetition (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`

**Test first:**

Add `syncHoldWaitsFourHundredMillisecondsThenRepeatsEveryHundred`. Start at `0 ms`, hold increase, and drive only `compose.mainClock`; do not sleep in wall-clock time:

```kotlin
increase.performTouchInput { down(center) }
advanceUi()
assertEquals(1, increases)

compose.mainClock.advanceTimeBy(399)
assertEquals(1, increases)
compose.mainClock.advanceTimeBy(1)
advanceUi()
assertEquals(2, increases)
compose.mainClock.advanceTimeBy(100)
advanceUi()
assertEquals(3, increases)

increase.performTouchInput { up() }
compose.mainClock.advanceTimeBy(500)
advanceUi()
assertEquals(3, increases)
assertEquals(75, offsetMs)
```

Run the focused test first and confirm Task 1 performs only the immediate adjustment.

**Implementation:**

Inside the enabled overlay's `onPress`, use `coroutineScope` to start one child repeat job after the immediate callback. The job delays for `SYNC_REPEAT_INITIAL_DELAY_MS`, invokes the latest callback, and initially delays `SYNC_REPEAT_INTERVAL_MS` between subsequent invocations. In parallel, the press coroutine awaits release/cancellation; once that wait ends, cancel and join the repeat job in `finally` so no delayed action can escape the gesture.

Add private timing constants beside the existing sync-label constants:

```kotlin
private const val SYNC_REPEAT_INITIAL_DELAY_MS = 400L
private const val SYNC_REPEAT_INTERVAL_MS = 100L
```

Do not refresh the label separately in the gesture layer. Every invocation must continue through the existing `VisualizerDeck` callback wrapper so `syncLabelGeneration` changes exactly once per real adjustment.

**Verify:** Assemble Android tests and run the focused `VisualizerDeckTest`. The 399/400/500 ms boundaries and release-stop assertion pass under the Compose test clock.

### Task 3: Accelerate repetition after 1.5 seconds (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`

**Test first:**

Add `syncHoldAcceleratesAtFifteenHundredMilliseconds`. Hold increase from `0 ms`, advance the main clock through the action at 1400 ms, and record the callback count. Advance to 1499 ms and assert no new callback, then cross 1500 ms and assert one callback. Finally advance 49 ms and then 1 ms, proving the next callback occurs at 1550 ms rather than 1600 ms:

```kotlin
compose.mainClock.advanceTimeBy(1_499)
advanceUi()
val beforeThreshold = increases
compose.mainClock.advanceTimeBy(1)
advanceUi()
assertEquals(beforeThreshold + 1, increases)
compose.mainClock.advanceTimeBy(49)
assertEquals(beforeThreshold + 1, increases)
compose.mainClock.advanceTimeBy(1)
advanceUi()
assertEquals(beforeThreshold + 2, increases)
```

Release the press and assert another 100 ms of clock advancement produces no callback. Run this test first and confirm Task 2 still waits 100 ms after the 1500 ms action.

**Implementation:**

Add:

```kotlin
private const val SYNC_REPEAT_ACCELERATION_THRESHOLD_MS = 1_500L
private const val SYNC_REPEAT_ACCELERATED_INTERVAL_MS = 50L
```

Track elapsed hold time inside the repeat job, starting at the 400 ms initial delay. After each repeat callback, select the next delay from the elapsed time: use 100 ms below 1500 ms and 50 ms at or above 1500 ms. Advance the elapsed counter by the chosen delay. Keep the 25 ms value step in `VisualizerSyncOffset` unchanged.

**Verify:** Assemble Android tests and run `VisualizerDeckTest`. The test distinguishes the accelerated 1550 ms callback from the former fixed-rate 1600 ms callback.

### Task 4: Verify release, cancellation, and out-of-bounds stopping (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`

**Test first:**

Add `syncHoldStopsOnCancelAndMovementOutside`. Use separate fixture runs or reset counters between these cases:

1. Hold through the first repeat, call `cancel()`, advance 500 ms, and assert the count remains unchanged.
2. Hold through the first repeat, move to `Offset(-1f, center.y)` outside the node, advance 500 ms, and assert the count remains unchanged.
3. Begin a hold, then use the deck's semantic `performClick()` to change modes while the pointer remains down; advance 500 ms and assert no further adjustment occurs after the sync overlay leaves composition.

Retain an assertion that the selected visualizer does not change during ordinary sync pressing. Import `androidx.compose.ui.geometry.Offset` only for the out-of-bounds case. Run the cases before altering production behavior.

**Implementation:**

Rely on `detectTapGestures`/`tryAwaitRelease` to report pointer cancellation and movement outside the input area, and keep repeat-job cleanup in `finally` so cancellation and disposal share the same path. If a test exposes a leaked repeat, fix only the job ownership or cleanup in the private corner control; do not add ViewModel state or lifecycle observers.

**Verify:** Assemble Android tests and run `VisualizerDeckTest`. All three cases remain stable for at least 500 ms of virtual time after cancellation.

### Task 5: Stop at limits without restarting on recomposition (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`

**Test first:**

Add `syncHoldStopsWhenTheOffsetReachesEitherLimit`. Start at `VisualizerSyncOffset.MAX_MS - 50`, hold increase for one second, and assert exactly two callbacks move the value to the maximum. Advance another second and assert the value and callback count remain unchanged; assert increase is disabled and radar is still selected. Repeat from `VisualizerSyncOffset.MIN_MS + 50` with decrease.

The timing tests from Tasks 2 and 3 already force recomposition after every callback. Retain their exact callback counts to prove recomposition does not restart the 400 ms delay or create duplicate repeat jobs.

**Implementation:**

Key the physical gesture's `pointerInput` with `enabled`, but not with the changing callback identity or current offset. When a callback reaches a boundary, recomposition changes `enabled` to false, which cancels the active pointer coroutine and its repeat child. Retain the existing disabled overlay that consumes touch without invoking the callback or cycling modes. Do not call a disabled/no-op callback merely to poll whether the limit was reached.

**Verify:** Assemble Android tests and run `VisualizerDeckTest`. Both directions stop exactly at `-2000 ms` and `+2000 ms`, their disabled states are exposed, and all timing tests retain deterministic counts.

### Task 6: Make opposite holds mutually exclusive (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`

**Test first:**

Add `newSyncPressCancelsTheOppositeActiveHold`. Fetch decrease and increase bounds in root coordinates, then inject two pointers through `compose.onRoot()`:

1. Put pointer 0 down at the decrease center and advance through at least one repeat.
2. Put pointer 1 down at the increase center while pointer 0 remains down.
3. Record the decrease count, advance 500 ms, and assert decrease remains frozen while increase continues.
4. Release pointer 1 and pointer 0, advance another 500 ms, and assert both counts remain frozen.

Use explicit pointer IDs with `down(pointerId, position)` and `up(pointerId)`. Confirm the test fails while both private controls can own independent repeat jobs.

**Implementation:**

Remember one `MutatorMutex` inside `VisualizerSyncControls` and pass it privately to both `SyncCornerButton` instances. Wrap each enabled overlay's entire `onPress` mutation—immediate callback, repeat job, and release wait—in `repeatMutator.mutate`. Equal-priority acquisition by the newly pressed direction cancels the previous direction's mutation and its child job. Keep the mutex private to this overlay; do not hoist it to `VisualizerDeck`, the ViewModel, or application state.

**Verify:** Assemble Android tests and run `VisualizerDeckTest`. The two-pointer test proves only the most recent direction repeats, and the single-pointer timing tests remain unchanged.

### Task 7: Preserve the Now Playing integration contract (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt`

**Test first:**

Extend `exposesSharedSyncControlsOnlyForVisualizerModes` with one physical increase press using `down(center)`, an immediate assertion that the real `NowPlayingScreen` callback count and offset changed by one step, and `up()` followed by an assertion that release did not add another step. Retain its existing geometry, decrease/increase/reset, mode-preservation, bands, and artwork assertions; adjust expected counts only for the newly added physical action.

Run the integration test before any integration production change. It should pass through the completed private control behavior, demonstrating that no new Now Playing API is required.

**Implementation:**

No production integration change is expected. If the test fails, correct only the private visualizer control behavior from Tasks 1–6. Keep `NowPlayingScreen`, `NocturneLApp`, `SettingsViewModel`, `TerminalPreferencesRepository`, `PlaybackConnection`, and audio analysis unchanged.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.NowPlayingVisualizerTest
```

The existing callback wiring applies the immediate press exactly once.

### Task 8: Document accelerated sync adjustment (2–5 min)

**Files:** `docs/_manual/visualizers.md`, `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Review the current sync instructions and release checklist. Confirm they describe only 25 ms selection and do not require hold timing, cancellation, acceleration, or a practical large-offset calibration.

**Implementation:**

In `docs/_manual/visualizers.md`, retain the three existing control bullets and add one concise bullet stating that holding minus or plus repeats the 25 ms adjustment and accelerates during a continued hold. Do not describe presets or imply automatic Bluetooth detection.

In `docs/testing/pixel-7-release-checklist.md`, update only the visualizer sync block to require:

- A quick tap changes exactly 25 ms and release does not double-adjust.
- Holding adjusts immediately, waits about 400 ms before repeating, and visibly accelerates after about 1.5 seconds.
- Release and dragging outside stop adjustment promptly.
- Holding stops cleanly at both existing limits without cycling modes.
- Moving from `0 ms` to approximately `+1400 ms` in the target Bluetooth vehicle feels quick and controllable.

Preserve the existing overlay, label, persistence, playback-safety, and calibration checks.

**Verify:** Run:

```powershell
rg -n "hold|25 ms|400 ms|1.5 seconds|1400 ms|-2000 ms|\+2000 ms" docs/_manual/visualizers.md docs/testing/pixel-7-release-checklist.md
```

Inspect the matching blocks for consistency and confirm presets remain absent.

### Task 9: Run final regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–8

**Test first:**

Review the scoped diff:

```powershell
git diff -- app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt docs/_manual/visualizers.md docs/testing/pixel-7-release-checklist.md
```

Confirm it contains no offset presets, shared button API changes, ViewModel/repository/playback changes, new dependencies, layout changes, haptics, or visualizer rendering changes.

**Implementation:**

Fix only failures introduced by this approved feature. Do not change `VisualizerSyncOffset`, callback signatures, persistence timing, the three-second sync label behavior, the visualizer mode order, or screenshot references unless verification identifies a genuine unintended visual regression in the scoped production change.

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

If a device or emulator is attached, also run `.\gradlew.bat connectedDebugAndroidTest`. On the Pixel 7, complete the updated hold-to-repeat checks with the target vehicle. Explicitly report connected and hardware checks as pending when unavailable.

## Definition of Done

- [ ] All tasks are completed in order.
- [ ] Each production behavior is covered by a test written before its implementation.
- [ ] Touch-down adjusts the offset immediately by one 25 ms step.
- [ ] A quick release does not cause a second adjustment, while semantic clicks still adjust once.
- [ ] Holding waits 400 ms before repeating every 100 ms, then repeats every 50 ms from 1500 ms onward.
- [ ] Release, cancellation, movement outside, mode changes, and disposal stop repetition immediately.
- [ ] Recomposition does not restart or duplicate an active repeat loop.
- [ ] Reaching `-2000 ms` or `+2000 ms` stops the hold and disables the applicable button.
- [ ] Starting the opposite direction cancels the previous active hold.
- [ ] Sync-control touches never cycle the visualization mode.
- [ ] The existing transient sync label refreshes once per real adjustment and fades after the final change.
- [ ] The existing ViewModel, persistence, playback, visualizer rendering, layout, and accessible one-step callback interfaces remain unchanged.
- [ ] Offset presets, device-specific offsets, automatic latency detection, haptics, and new UI are not added.
- [ ] The visualizer manual and Pixel 7 checklist describe and verify the approved behavior.
- [ ] Unit tests, Android-test assembly, screenshot validation, lint, and debug assembly pass.
- [ ] Connected tests and the target-vehicle check pass when hardware is available, or are explicitly reported pending.
- [ ] `git diff --check` passes and no unrelated files are modified.
