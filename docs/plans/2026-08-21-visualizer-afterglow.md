# Visualizer Afterglow Implementation Plan

**Date:** 2026-08-21  
**Design doc:** `docs/specs/2026-08-21-visualizer-afterglow-design.md`  
**Status:** Ready for review

## Overview

Add a short CRT-style persistence effect to the existing radar and spectrum-band visualizers without changing their public composable interface, audio analysis, layout, palette, or controls. A pure immutable reducer will retain bounded normalized history, a Compose monotonic frame loop will age it for at most 250 ms, and the existing Canvas renderer will draw dim radar arms and falling ghost bar segments behind the live visualization only while CRT effects and active analysis are both enabled.

## Fixed Implementation Decisions

- Keep `TerminalVisualizerScene(mode, frame, effectsEnabled, modifier)` unchanged.
- Put the pure afterglow state and reducer in a new `VisualizerAfterglow.kt` file in the existing visualizer UI package; do not add state to `AudioAnalysisFrame` or the analyzer.
- Use one `AFTERGLOW_DURATION_NANOS = 250_000_000L` constant for both modes.
- Retain at most eight prior radar sweep samples. Add a sample only when an active frame with a new, strictly increasing `frameId` arrives; repeated display frames age history without duplicating it.
- Normalize every retained radar angle to `0f..<360f`. Draw independent historical arms rather than connecting angles, so the 360-to-0 transition cannot draw across the circle.
- Fade radar samples linearly from maximum alpha `.55f` to zero, draw them in `PhosphorDim` with the existing `1f` sweep width, oldest first, and retain the existing live `PhosphorBright` arm at alpha `.8f`.
- Keep one normalized `BandAfterglow` envelope per current band. A live value at or above its retained envelope replaces the baseline immediately; a falling value retains the former height, moves the envelope downward linearly, and fades it to zero over no more than 250 ms.
- Draw only ghost segments strictly above the live bar, in `PhosphorDim` with maximum alpha `.60f`. Keep existing live `Phosphor` segments and the current `PhosphorBright` peak line unchanged and above the ghost layer.
- Sanitize retained band values to finite `0f..1f`; a band-count change rebuilds all envelopes from the current live values with no visible ghost.
- Drive elapsed time with `withFrameNanos`, read the latest analysis frame through `rememberUpdatedState`, and never use wall-clock time, `delay`, an infinite transition, or a coroutine per band.
- Store normalized angles and band levels, not pixel coordinates. Also key/reset state on measured Canvas size so the approved resize-clear behavior is explicit.
- Clear or suppress all retained output immediately for ART mode, `effectsEnabled = false`, `IDLE`, `UNAVAILABLE`, a non-increasing frame ID, a size change, or a mode change.
- Add no setting, dependency, shader, blur, semantic property, test tag, callback, or accessibility announcement for this decorative effect.

## Tasks

### Task 1: Add the bounded radar decay reducer (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAfterglowTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAfterglow.kt`

**Test first:**

Create `VisualizerAfterglowTest` with focused tests for a pure radar update function:

```kotlin
@Test fun firstRadarFrameHasNoInventedTrail()
@Test fun newRadarFramesRetainAtMostEightPriorAngles()
@Test fun repeatedFrameIdAgesWithoutDuplicatingSweep()
@Test fun radarTrailFadesMonotonicallyAndExpiresAt250Milliseconds()
@Test fun radarAnglesNormalizeAcross360WithoutConnectedWrapGeometry()
@Test fun nonIncreasingRadarFrameIdStartsFresh()
```

Use angles around the wrap boundary (`358f`, `0f`, `2f`), explicit nanosecond deltas, and more than eight sequential IDs. Assert the first frame has an empty trail, all stored angles lie in `0f..<360f`, opacity is in `0f..0.28f`, opacity falls as age increases, the oldest samples are discarded first, repeated IDs do not add samples, and a 250 ms or larger gap leaves no historical sample. Run the focused test and confirm it fails because the afterglow types do not exist.

**Implementation:**

Add immutable internal radar state containing the current frame ID/angle and a list of `RadarAfterglowSample(angleDegrees, ageNanos)`. Add a pure update function that:

1. Clamps negative elapsed time to zero.
2. Ages and removes existing samples before accepting new input.
3. On a strictly newer frame ID, moves the previous live angle into history, then records the new live ID/angle.
4. Retains only the newest eight historical samples.
5. Resets to the incoming angle with no history for a missing current sample or non-increasing frame ID.

Expose each retained sample's linear `alpha = .28f * (1f - age / duration)` for rendering. Do not introduce Compose types into this file.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerAfterglowTest'
```

All radar reducer cases pass.

### Task 2: Add falling band-envelope decay (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAfterglowTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAfterglow.kt`

**Test first:**

Add pure reducer cases:

```kotlin
@Test fun risingBandReplacesEnvelopeWithoutVisibleGhost()
@Test fun fallingBandRetainsThenLowersAndFadesFormerHeight()
@Test fun bandEnvelopeNeverFallsBelowLiveValue()
@Test fun bandGhostExpiresAt250Milliseconds()
@Test fun bandCountChangeRebuildsWithoutGhosts()
@Test fun malformedBandValuesAreFiniteAndClamped()
```

Start with a high band vector, drop selected bands, and update at `0`, `125`, `249`, and `250` ms. Assert retained heights and alpha decrease monotonically, retained values never fall below live values, alpha stays in `0f..0.30f`, and nothing remains visible at 250 ms. Include empty lists, `NaN`, infinities, values below zero and above one. Confirm these tests fail before implementation.

**Implementation:**

Add immutable `BandAfterglow` entries holding the decay start level, retained level, and age. Add a pure band update function that sanitizes the current values and applies these rules per index:

- If the live level meets or exceeds the retained level, use it as the new baseline with age zero and no visible ghost.
- When the live level falls, preserve the prior retained level as the decay start, advance age, calculate a linearly descending retained level, and clamp it to at least the current live level.
- Calculate `.30f * (1f - age / duration)` only when retained level is above the live level.
- At or beyond 250 ms, collapse the envelope to the current live value with zero ghost alpha.
- If vector size changes, return one baseline entry per current value and no ghost output.

Keep the model normalized and independent of Canvas dimensions and spectrum segment height.

**Verify:** Run the focused `VisualizerAfterglowTest`; all radar and band cases pass.

### Task 3: Enforce unified lifecycle resets (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAfterglowTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAfterglow.kt`

**Test first:**

Add tests around one top-level `updateVisualizerAfterglow` reducer using small active `AudioAnalysisFrame` fixtures. Verify that populated state becomes empty when any one of these changes:

- mode changes from RADAR or BANDS to another mode;
- `effectsEnabled` becomes false;
- status becomes `IDLE` or `UNAVAILABLE`;
- the measured `IntSize` changes;
- frame ID becomes equal to or less than the accepted ID.

Verify re-entering an eligible active mode begins with the current frame as a fresh baseline and no visible trail. Also assert RADAR updates never retain band envelopes and BANDS updates never retain radar samples.

**Implementation:**

Create immutable `VisualizerAfterglowState` with the active mode, last measured size key, radar state, and band envelopes, plus an `Empty` value. Implement a top-level reducer accepting state, mode, current frame, effects flag, size key, and elapsed nanoseconds. Gate eligibility before delegating to the radar or band updater; return `Empty` immediately for every ineligible or reset case. Keep ART as a reset-only mode.

Use `androidx.compose.ui.unit.IntSize` only at the composable boundary; pass a simple width/height value object or two integers into the pure reducer so JVM unit tests remain free of Android runtime behavior.

**Verify:** Run the focused unit test suite. All lifecycle, isolation, malformed-input, and decay tests pass.

### Task 4: Add the monotonic Compose frame driver (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`

**Test first:**

Extend `VisualizerSourceGuardTest` to read `TerminalVisualizers.kt` and require the afterglow driver to contain `withFrameNanos` and `rememberUpdatedState`. Reject `System.currentTimeMillis`, `System.nanoTime`, `elapsedRealtime`, `rememberInfiniteTransition`, and any `delay(` call in this renderer file. Confirm the positive source assertions fail before integration.

**Implementation:**

Inside `TerminalVisualizerScene`:

- Track the measured scene size with `Modifier.onSizeChanged` without changing the outer modifier order, background, border, tag, or dimensions.
- Hold immutable `VisualizerAfterglowState` in `remember` state and keep the latest `frame` available through `rememberUpdatedState`.
- Use one `LaunchedEffect(mode, effectsEnabled, frame.status, measuredSize)` loop. While RADAR or BANDS is active, effects are enabled, status is `ACTIVE`, and size is non-zero, call `withFrameNanos`, calculate a non-negative delta from the prior frame time, and pass the latest analysis frame into the reducer.
- Reset state before returning from any ineligible branch. Rendering must also ignore retained output whenever eligibility is false so disabling effects, pausing, unavailable analysis, and mode changes are visually immediate even before the effect coroutine resumes.
- Do not restart the effect for every audio `frameId`; the latest-state read lets one display-clock loop age history continuously and sample each distinct analysis frame once.

At this task, pass the state into private drawing helpers but do not draw the new layers yet.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerSourceGuardTest' --tests '*VisualizerAfterglowTest'
.\gradlew.bat assembleDebugAndroidTest
```

Source contracts and pure tests pass, and the Compose renderer compiles.

### Task 5: Project and render radar trail arms (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`

**Test first:**

Extract the existing sweep endpoint calculation behind a pure `radarSweepEndpoint(center, radius, sweepDegrees)` helper and add tests for `0f`, `90f`, `180f`, `270f`, `358f`, `360f`, and `-2f`. Require normalized-equivalent angles to return equivalent endpoints and every endpoint to remain on the requested radius within floating-point tolerance. Confirm the new helper test fails before implementation.

**Implementation:**

Use the tested helper for both historical and live radar arms. In the RADAR Canvas branch:

1. Keep grid circles, energy rings, spokes, and transient echo in their existing order and unchanged.
2. Draw eligible retained samples oldest-to-newest using `PhosphorDim.copy(alpha = sample.alpha)`, the center, the outer grid radius, and stroke width `1f`.
3. Draw the current live sweep last using its existing `PhosphorBright.copy(alpha = .8f)` and `1f` width.

Do not trail any other radar geometry, interpolate between samples, connect arms, or add blur.

**Verify:** Run the focused `VisualizerGeometryTest` and `VisualizerAfterglowTest`; all existing geometry plus the new projection cases pass.

### Task 6: Project and render falling ghost bar segments (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`

**Test first:**

Add a normalized-band overload or a focused `spectrumGhostGeometry` helper and test that:

- retained level equal to live level creates no ghost segments;
- a retained level above live creates only segment indexes above the live bar and no duplicate live segments;
- zero live level can show a valid unexpired ghost;
- ghost rectangles use the same inset, gap, width, 4 px segment height, and 6 px pitch as live bars;
- empty, tiny, and non-square canvases produce finite, bounded geometry.

Confirm the new tests fail before implementation.

**Implementation:**

Reuse the existing spectrum column calculations rather than duplicating spacing math. For each band, convert current and retained normalized levels to segment counts at the current Canvas height, then draw only indexes from `liveSegments` until `retainedSegments` in `PhosphorDim.copy(alpha = envelope.alpha)`. Draw the existing live segment rectangles and current peak line afterward without changing their colors, alpha, or dimensions.

If rounding maps live and retained values to the same segment count, draw no ghost for that band. Never draw below the live bar or above the existing inset.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerGeometryTest' --tests '*VisualizerAfterglowTest'
.\gradlew.bat assembleDebugAndroidTest
```

All geometry/state tests pass and Android tests compile.

### Task 7: Preserve scene contracts and clock lifecycle (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizersTest.kt`

**Test first:**

Extend the Compose tests using `compose.mainClock.autoAdvance = false` and mutable mode/frame/effects state. Cover these observable contracts:

- an active RADAR scene remains tagged `visualizer-radar`, has no click action, and keeps scanlines when effects are on;
- switching effects off removes scanlines, remains stable while the test clock advances beyond 250 ms, and does not change the scene tag or size;
- switching RADAR to BANDS keeps the existing `visualizer-bands` contract and does not expose a second interactive node;
- replacing ACTIVE with `UNAVAILABLE` immediately displays `SIGNAL UNAVAILABLE`; advancing the clock does not restore stale visualizer content;
- restoring ACTIVE after IDLE/UNAVAILABLE leaves the scene mounted and stable from a fresh lifecycle.

The pure reducer tests remain the assertion point for invisible retained state; do not add afterglow semantics or test tags merely to inspect decorative Canvas history.

**Implementation:**

Make only lifecycle corrections exposed by these tests. Preserve the existing unavailable fallback, outer border/background, scanline placement, tags, size, and parent-owned click behavior. Do not alter `VisualizerDeck` or sync controls.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.visualizer.TerminalVisualizersTest
```

If no device/emulator is attached, report the connected run as pending; Android-test assembly must still pass.

### Task 8: Add deterministic afterglow screenshot fixtures (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/*.png`

**Test first:**

Add two `@PreviewTest` previews at 320-by-320 dp that use a small internal stateless Canvas rendering helper fed with explicit, immutable afterglow state:

- `VisualizerRadarAfterglowPreview`: an active radar frame with the live arm at a distinct angle and several retained arms at increasing ages, including a wrap-adjacent angle.
- `VisualizerBandsAfterglowPreview`: an active bands frame whose retained envelopes exceed a varied subset of current values.

The helper must be the same renderer called by `TerminalVisualizerScene`; it may accept afterglow state internally, but the public scene signature remains unchanged. Run screenshot validation and confirm it fails because the new reference images do not exist.

**Implementation:**

Generate the references and inspect them before retaining changes:

- Radar has no trail on rings, spokes, or transient echo; historical arms are visibly dimmer than the live arm and follow behind it across wraparound.
- Bands show dim segments only above shorter live bars; live bars and peak markers remain dominant.
- Both remain black/phosphor, crisp, restrained, and free of blur or new colors.
- No unrelated screenshot reference changes are kept.

Resolve the exact generated PNG names after the screenshot task reports them; do not manually invent or rename hashed reference filenames.

**Verify:** Run:

```powershell
.\gradlew.bat updateDebugScreenshotTest
.\gradlew.bat validateDebugScreenshotTest
git status --short -- app/src/screenshotTest app/src/screenshotTestDebug/reference
```

Only the preview source and two reviewed reference images are new or changed.

### Task 9: Update focused device acceptance checks (2–5 min)

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Expand only the existing visualizer checklist block with unchecked requirements that:

- the radar sweep has a slight trail while rings, spokes, and transient echoes remain crisp;
- falling band ghosts last about 250 ms, remain dimmer than live segments, and do not smear peak markers;
- pausing, unavailable analysis, switching modes, resizing/recreating the screen, and disabling effects clear afterglow immediately;
- effects-off radar and bands remain identical to their prior crisp behavior apart from existing scanline removal;
- a 30-minute visualizer run shows no new animation stutter, excessive heat, unreasonable battery drain, or audible playback glitch.

**Implementation:**

Run the new visual checks on the target Pixel 7 with quiet, balanced, percussion-heavy, and rapidly changing music. Record any excessive persistence, frozen ghost, smear, mode carryover, or performance regression as a blocker. Do not add tuning settings while calibrating; adjust only the fixed bounded alpha/decay constants if the approved restrained effect fails visual review.

**Verify:** Every new checklist item is checked or explicitly reported as pending/blocking when the device or suitable audio is unavailable.

### Task 10: Run regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–9

**Test first:**

Run focused source checks:

```powershell
rg -n 'System\.currentTimeMillis|System\.nanoTime|elapsedRealtime|rememberInfiniteTransition|infiniteRepeatable|RenderEffect|BlurEffect' app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer
rg -n 'afterglow|Afterglow|withFrameNanos' app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer
git diff --check
```

The first command returns no matches. Inspect the second command to confirm the effect is confined to the presentation package and its focused tests.

**Implementation:**

Fix only failures introduced by the approved afterglow work. Do not change audio capture/analysis, `AudioAnalysisFrame`, visualizer synchronization, settings persistence, mode ordering, layout, controls, dependencies, theme colors, or unrelated screenshots. Preserve all user changes already present in the worktree.

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

If a device/emulator is attached, also run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Confirm the final diff contains only the approved design/plan, afterglow model and rendering, focused tests/previews/references, and checklist changes. Connected/device-only checks must be explicitly reported as pending if the required hardware is unavailable.

## Definition of Done

- [ ] All tasks completed in order, with each new behavior introduced after its failing test or contract check.
- [ ] Radar retains at most eight independently drawn prior sweep arms for no longer than 250 ms.
- [ ] Radar history fades behind the live arm and never trails grids, energy rings, spokes, or transient echoes.
- [ ] Each band's retained envelope falls, fades, never drops below live height, and produces only ghost segments above the live bar.
- [ ] Live bars, live sweep, and peak markers remain brighter than every afterglow layer.
- [ ] ART, effects off, IDLE, UNAVAILABLE, mode changes, size changes, band-count changes, and non-increasing frame IDs clear history safely.
- [ ] Timing uses the Compose monotonic frame clock and remains independent of display refresh rate.
- [ ] `TerminalVisualizerScene` keeps its existing signature, tags, dimensions, fallback, border, scanlines, and non-clickable behavior.
- [ ] No audio, synchronization, settings, dependency, palette, shader, blur, semantic, or control changes are introduced.
- [ ] Pure unit tests, Android-test assembly, screenshot validation, lint, and debug build pass.
- [ ] Connected Compose tests and Pixel 7 visual/performance checks pass or are explicitly reported as pending.
- [ ] No unrelated files are modified.
