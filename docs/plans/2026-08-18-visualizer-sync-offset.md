# Visualizer Sync Offset Implementation Plan

**Date:** 2026-08-18  
**Design doc:** `docs/specs/2026-08-18-visualizer-sync-offset-design.md`  
**Status:** Ready for review

## Overview

Add one persisted visualizer timing offset, adjustable from the active Now Playing visualizer with minus, reset, and plus controls. The terminal settings layer will own the durable value, the existing playback connection will forward it to the application-scoped analysis repository, and the PCM sink will apply it when choosing the playback-relative analysis window. All work is test-first and leaves audible playback, album art, and individual visualizer renderers unchanged.

## Fixed Implementation Decisions

- Store the offset as an `Int` number of milliseconds under `visualizer_sync_offset_ms` in the existing `terminal_preferences` SharedPreferences file.
- Put the reusable range, step, clamping, adjustment, and signed-label rules in `ca.stewark.nocturnel.visualizer.VisualizerSyncOffset`; this avoids making the PCM layer depend on UI packages.
- Use `MIN_MS = -500`, `MAX_MS = 1000`, `STEP_MS = 25`, and `DEFAULT_MS = 0` exactly.
- Forward settings changes through `PlaybackConnection.setVisualizerSyncOffsetMs`; do not add the offset to `PlaybackUiState`, MediaSession commands, playback snapshots, or Room.
- Apply the offset in `PcmAnalysisBufferSink` as `baseSamplesAhead + offsetSamples`, clamped to at least zero. Positive values therefore select older PCM; negative values select the newest available decoded PCM when they exhaust the base lead.
- Keep the sync controls in a small stateless `VisualizerSyncControls` composable in `VisualizerDeck.kt`, using the existing bracket-button components and stable test tags.
- Use `LaunchedEffect(settings.visualizerSyncOffsetMs)` in `NocturneLApp` to forward the initial persisted value and every later change without performing mutation directly during composition.

## Tasks

### Task 1: Define the sync-offset value rules (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/VisualizerSyncOffsetTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/VisualizerSyncOffset.kt`

**Test first:**

Create `VisualizerSyncOffsetTest` with assertions equivalent to:

```kotlin
assertEquals(0, VisualizerSyncOffset.clamp(0))
assertEquals(-500, VisualizerSyncOffset.clamp(-999))
assertEquals(1000, VisualizerSyncOffset.clamp(9_999))
assertEquals(25, VisualizerSyncOffset.increase(0))
assertEquals(-25, VisualizerSyncOffset.decrease(0))
assertEquals(1000, VisualizerSyncOffset.increase(1000))
assertEquals(-500, VisualizerSyncOffset.decrease(-500))
assertEquals("+150 ms", VisualizerSyncOffset.label(150))
assertEquals("-25 ms", VisualizerSyncOffset.label(-25))
assertEquals("0 ms", VisualizerSyncOffset.label(0))
```

Also assert the public constants are exactly `DEFAULT_MS = 0`, `MIN_MS = -500`, `MAX_MS = 1000`, and `STEP_MS = 25`. Run the test and confirm it fails because the policy does not exist.

**Implementation:**

Add an `object VisualizerSyncOffset` containing the four constants and pure `clamp`, `increase`, `decrease`, and `label` functions. `increase` and `decrease` must call `clamp`; `label` must clamp before formatting and use an explicit `+` only for positive values. Add no output-device detection or per-mode state.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerSyncOffsetTest'
```

The offset policy tests pass.

### Task 2: Persist and sanitize the offset (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepositoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/TerminalPreferencesRepository.kt`

**Test first:**

Extend `TerminalPreferencesRepositoryTest` using a unique test preference name and clearing it before each scenario. Assert:

- A new repository exposes `visualizerSyncOffsetMs.value == 0`.
- `setVisualizerSyncOffsetMs(175)` is visible immediately and a newly constructed repository with the same name loads `175`.
- Persisted values below `-500` and above `1000` load as the respective limit.
- A wrong-typed stored value, such as a `String` under `visualizer_sync_offset_ms`, loads as `0` instead of throwing.
- Calling the setter with an out-of-range value updates the in-memory flow and persisted value to the clamped limit.

Run the focused connected test first and confirm the new assertions fail.

**Implementation:**

In `TerminalPreferencesRepository`, add a private `MutableStateFlow<Int>` initialized by a safe read:

```kotlin
runCatching { preferences.getInt(VISUALIZER_SYNC_OFFSET_MS, VisualizerSyncOffset.DEFAULT_MS) }
    .getOrDefault(VisualizerSyncOffset.DEFAULT_MS)
    .let(VisualizerSyncOffset::clamp)
```

Expose it as `StateFlow<Int>`. Add `setVisualizerSyncOffsetMs(offsetMs: Int)` that clamps once, writes the clamped integer with `apply()`, and always updates the in-memory flow for the current session. Keep the existing effects preference unchanged.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.settings.TerminalPreferencesRepositoryTest
```

The instrumentation test passes when a device or emulator is attached.

### Task 3: Expose adjustment operations from settings state (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/settings/SettingsViewModelTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/settings/SettingsViewModel.kt`

**Test first:**

Create an instrumentation test that clears the real `terminal_preferences` file, constructs `SettingsViewModel(ApplicationProvider.getApplicationContext())`, and asserts:

```kotlin
assertEquals(0, viewModel.state.value.visualizerSyncOffsetMs)
viewModel.increaseVisualizerSyncOffset()
assertEquals(25, viewModel.state.value.visualizerSyncOffsetMs)
viewModel.decreaseVisualizerSyncOffset()
assertEquals(0, viewModel.state.value.visualizerSyncOffsetMs)
viewModel.resetVisualizerSyncOffset()
assertEquals(0, viewModel.state.value.visualizerSyncOffsetMs)
```

Drive repeated increases/decreases to verify both limits, then construct a second view model and verify it restores the last value. Clear the preference in `finally` so the test does not leak state.

**Implementation:**

Add `visualizerSyncOffsetMs: Int = VisualizerSyncOffset.DEFAULT_MS` to `TerminalSettingsState`. Preserve that value whenever effects state is resolved. Add `increaseVisualizerSyncOffset`, `decreaseVisualizerSyncOffset`, and `resetVisualizerSyncOffset`; each computes the next value with `VisualizerSyncOffset`, persists it through the repository, and updates `_state` immediately while retaining the three effects fields.

Do not add offset controls to `SettingsScreen`; the approved controls belong beside the active visualizer.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.settings.SettingsViewModelTest
```

The view-model behavior passes on an attached device or emulator.

### Task 4: Select PCM with positive and negative offsets (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/PcmAnalysisBufferSinkTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/PcmAnalysisBufferSink.kt`

**Test first:**

Extend the existing identifiable 1 kHz sample fixture. After capturing samples `1..10` with reported playback at sample 8, assert:

- At `0 ms`, a four-sample copy returns samples `5..8` as today.
- At `+2 ms`, it returns samples `3..6`.
- At `-2 ms`, it returns samples `7..10`.
- At `-500 ms`, it clamps to the newest available window rather than passing a negative `samplesBehind` value.
- At `+1000 ms` before enough post-reset PCM exists, `copyPlaybackAligned` returns `false`.
- Equivalent fixtures at 2 kHz prove milliseconds are converted with the active sample rate rather than a fixed multiplier.
- `playbackAlignedSampleCount()` changes when the offset changes so the repository analyzes the newly selected window on its next tick.

Run the sink test and confirm the offset cases fail.

**Implementation:**

Add a volatile offset field and:

```kotlin
fun setVisualizerSyncOffsetMs(offsetMs: Int)
```

Clamp the input with `VisualizerSyncOffset`. Replace the current samples-ahead calculation with an adjusted calculation:

```text
baseSamplesAhead = max(0, capturedThroughPositionUs - playbackPositionUs) * sampleRateHz / 1_000_000
offsetSamples = visualizerSyncOffsetMs * sampleRateHz / 1_000
adjustedSamplesBehind = max(0, baseSamplesAhead + offsetSamples)
```

Use the same adjusted value in both `copyPlaybackAligned` and `playbackAlignedSampleCount`. Retain all existing PCM decoding, timing reset, buffer passivity, and audio-thread allocation behavior.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*PcmAnalysisBufferSinkTest'
```

All old and new sink tests pass.

### Task 5: Apply offset changes through the analysis repository (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/AudioAnalysisRepositoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/visualizer/AudioAnalysisRepository.kt`

**Test first:**

Add a coroutine-test scenario that activates playback and the consumer, captures enough PCM containing distinguishable older and newer regions, obtains an active frame at zero offset, calls `setVisualizerSyncOffsetMs(25)`, advances one 33 ms interval, and verifies a new active frame is published from the newly selected sample count without toggling either lifecycle flag. Also assert that setting `-500` is accepted safely and that disabling the consumer still returns `IDLE` exactly as before.

**Implementation:**

Expose:

```kotlin
fun setVisualizerSyncOffsetMs(offsetMs: Int) {
    bufferSink.setVisualizerSyncOffsetMs(offsetMs)
}
```

Do not restart the worker, reset `AudioAnalyzer`, clear smoothing, or publish a synthetic frame from this setter. The existing `playbackAlignedSampleCount()` freshness check must cause the next polling iteration to analyze the adjusted window.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AudioAnalysisRepositoryTest'
```

Repository lifecycle tests and the live-offset test pass.

### Task 6: Add visualizer sync controls and isolate their taps (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeck.kt`

**Test first:**

Extend `VisualizerDeckTest` with mutable offset state and callbacks. Assert:

- No node tagged `visualizer-sync-controls` exists in art mode.
- Entering radar displays `visualizer-sync-controls`, `visualizer-sync-decrease`, `visualizer-sync-reset`, and `visualizer-sync-increase`.
- The center text is `VIS SYNC +150 MS` for `150`, includes accessible reset/current-value semantics, and invokes reset.
- Minus and plus invoke their callbacks but leave the mode on radar; their clicks must not reach the deck's mode-cycle click handler.
- Minus is disabled at `-500`, plus is disabled at `1000`, and both are enabled between the limits.
- Controls remain visible while cycling radar, bands, and scope, then disappear on returning to art.

Run Android-test assembly before implementation and confirm the new API/test references fail.

**Implementation:**

Extend `VisualizerDeck` with `syncOffsetMs`, `onDecreaseSyncOffset`, `onIncreaseSyncOffset`, and `onResetSyncOffset`. Add a stateless internal `VisualizerSyncControls` aligned to `Alignment.BottomCenter`, using a compact `Row`, `BracketIconButton` for minus/plus, and `BracketButton` for the center reset value. Use `VisualizerSyncOffset.label(syncOffsetMs)`, uppercase terminal text, explicit accessibility descriptions, and the stable tags above. Determine enabled state from `MIN_MS` and `MAX_MS`.

Compose the controls after the visualizer scene only when `mode != ART`. Preserve the deck's existing mode state, temporary top-right mode label, and activation lifecycle.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
```

When a device is attached, run:

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.visualizer.VisualizerDeckTest
```

All deck interaction and accessibility assertions pass.

### Task 7: Thread the controls through Now Playing (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/NowPlayingVisualizerTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/NowPlayingScreen.kt`

**Test first:**

Extend `NowPlayingVisualizerTest` to compose the screen with `visualizerSyncOffsetMs = 75` and counting callbacks. Enter radar, assert `VIS SYNC +75 MS` is displayed, click each control, and assert the corresponding callback fires while radar remains selected. Verify controls are absent before entering a visualizer and after cycling back to art.

**Implementation:**

Add these parameters to `NowPlayingScreen`, with no-op/default values so unrelated previews and UI tests remain source-compatible:

```kotlin
visualizerSyncOffsetMs: Int = VisualizerSyncOffset.DEFAULT_MS
onDecreaseVisualizerSyncOffset: () -> Unit = {}
onIncreaseVisualizerSyncOffset: () -> Unit = {}
onResetVisualizerSyncOffset: () -> Unit = {}
```

Pass them directly to `VisualizerDeck`. Do not change artwork, metadata, favorites, seek, transport, queue, or layout behavior outside the square.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
```

Run the focused connected test when a device is available.

### Task 8: Wire persisted settings to the analyzer and UI (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/visualizer/VisualizerWiringTest.kt`, `app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt`

**Test first:**

Extend `VisualizerWiringTest` to require:

```kotlin
"fun setVisualizerSyncOffsetMs(offsetMs: Int)" in connection
"app.audioAnalysis.setVisualizerSyncOffsetMs(offsetMs)" in connection
"LaunchedEffect(settings.visualizerSyncOffsetMs)" in ui
"playback.setVisualizerSyncOffsetMs(settings.visualizerSyncOffsetMs)" in ui
"visualizerSyncOffsetMs = settings.visualizerSyncOffsetMs" in ui
"onDecreaseVisualizerSyncOffset = settingsViewModel::decreaseVisualizerSyncOffset" in ui
"onIncreaseVisualizerSyncOffset = settingsViewModel::increaseVisualizerSyncOffset" in ui
"onResetVisualizerSyncOffset = settingsViewModel::resetVisualizerSyncOffset" in ui
```

Also assert the offset is absent from `PlaybackUiState`, `PlaybackSnapshot`, and database schema files. Run the source guard first and confirm it fails on missing wiring.

**Implementation:**

Add `PlaybackConnection.setVisualizerSyncOffsetMs` as a direct delegation to the application repository. In `NocturneLApp`, add a `LaunchedEffect` keyed by the settings offset that forwards it through the connection. Pass the current offset and three settings-view-model callbacks only to `NowPlayingScreen`.

Do not collect another preference flow, create another repository instance in the playback service, or add high-frequency work to the MediaController refresh loop.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerWiringTest'
.\gradlew.bat compileDebugKotlin
```

The wiring guard and Kotlin compilation pass.

### Task 9: Add deterministic screenshot coverage (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/*.png`

**Test first:**

Add one screenshot preview that layers a fixed active terminal visualizer scene with `VisualizerSyncControls(syncOffsetMs = 150, ...)` inside the same square. Run screenshot validation and confirm it fails because the new reference does not exist.

**Implementation:**

Generate and inspect only the new representative reference. Confirm the bottom overlay reads `[-]  VIS SYNC +150 MS  [+]`, remains legible over the visualizer, does not collide with the temporary top-right mode label area, and preserves the approved terminal-green styling. Do not update unrelated references.

**Verify:** Run:

```powershell
.\gradlew.bat updateDebugScreenshotTest
.\gradlew.bat validateDebugScreenshotTest
```

Screenshot validation passes with only the reviewed sync-control reference added.

### Task 10: Extend the Pixel 7 release check (2–5 min)

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Add unchecked checklist entries for:

- Minus/plus changing all three visualizers by 25 ms per tap.
- Reset returning the displayed value to `0 ms`.
- Limit behavior at `-500 ms` and `+1000 ms`.
- Wired-output calibration and Bluetooth-earbud calibration using an obvious percussion track.
- The selected offset surviving a full app close/reopen.
- Pause, seek, track transition, and fresh visualizer activation at a large positive offset.
- Confirmation that changing the offset causes no audible seek, interruption, or glitch.

**Implementation:**

Run the new checks on the Pixel 7. Record the tested build identifier in the release notes or PR description. Treat any audible playback change, cross-mode inconsistency, persistence failure, or unsafe buffer behavior as a blocker rather than adjusting the approved range or step.

**Verify:** Every new checklist item is checked or explicitly reported as a release blocker.

### Task 11: Run regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–10

**Test first:**

Run focused scope guards:

```powershell
rg -n 'visualizerSyncOffset' app/src/main/java/ca/stewark/nocturnel/playback/PlaybackModels.kt app/src/main/java/ca/stewark/nocturnel/playback/PlaybackStateRepository.kt app/src/main/java/ca/stewark/nocturnel/data app/schemas
rg -n 'Bluetooth|AudioDevice|AudioManager' app/src/main/java/ca/stewark/nocturnel
rg -n 'visualizerSyncOffsetMs' app/src/main/java/ca/stewark/nocturnel/ui/settings app/src/main/java/ca/stewark/nocturnel/ui/NocturneLApp.kt app/src/main/java/ca/stewark/nocturnel/visualizer app/src/main/java/ca/stewark/nocturnel/playback/PlaybackConnection.kt
```

The first two commands return no feature-related matches. The third shows only the approved settings-to-analysis path.

**Implementation:**

Fix only failures introduced by this feature. Do not add device-specific settings, automatic latency detection, audio timing changes, visualizer-mode persistence, settings-screen controls, database changes, or unrelated refactors.

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

If a device is attached, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Confirm only the approved sync-offset implementation, tests/reference, design, plan, and Pixel 7 checklist are modified.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] Every production behavior was introduced after a failing test or contract check.
- [ ] The offset defaults to 0 ms, adjusts by 25 ms, clamps to -500 through +1000 ms, and persists across app recreation.
- [ ] Positive offsets select older PCM; negative offsets select newer available PCM and clamp safely at the newest decoded sample.
- [ ] Radar, spectrum, and oscilloscope consume the same adjusted analysis frame.
- [ ] Sync controls appear only in active visualizer modes, reset from the center value, expose accessible actions, and do not cycle the deck when tapped.
- [ ] Offset changes take effect on the next analysis cycle without restarting analysis or changing audible playback.
- [ ] Album art, playback state, playback snapshots, Room schemas, and individual renderer APIs remain unchanged.
- [ ] Unit tests, Android-test assembly, connected tests when available, screenshot validation, lint, and debug assembly pass.
- [ ] Pixel 7 wired/Bluetooth calibration and persistence checks pass.
- [ ] No unplanned files are modified.
