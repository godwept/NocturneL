# Portrait Full-Screen Visualizers Redesign Implementation Plan

**Date:** 2026-09-23  
**Design doc:** `docs/specs/2026-09-23-portrait-full-screen-visualizers-design.md`  
**Status:** Ready for review

## Overview

Redesign only the expanded Radar and Frequency Grid presentation. Full-screen Grid will use a portrait-native square-cell geometry while the existing NOW Grid keeps its current 30×30 square geometry. Full-screen Radar will render on the full viewport, keep its existing centered circular core, extend the sweep to the viewport boundary with a dim afterglow wake, and replace the old top/bottom margin glow with bounded center-outward transient pulses. The shared `TerminalVisualizerScene` remains the rendering path; an internal `expanded` flag selects the full-screen-only geometry/effects. Spectrum, audio analysis, sync, navigation, system bars, and NOW behavior remain unchanged.

Because these changes all live in the same visualizer subsystem and can remain atomic, implement them as one TDD batch and trigger CI once after the full batch is locally green.

## Tasks

### Task 1: Change the full-screen sizing contract

**Files:**  
`app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizerTest.kt`  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizer.kt`

**Test first:**

Replace `squareModesAreCenteredAndBandsFillTheViewport` with a test named `audioModesUseTheFullViewport`.

Render `FullScreenVisualizer` inside a 240 dp × 480 dp host and assert, for RADAR, GRID, and BANDS, that the visualizer node's bounds match the `full-screen-visualizer` root width and height.

Keep the existing swipe/Exit assertions unchanged.

The test must fail initially because RADAR and GRID are currently sized to `min(maxWidth, maxHeight)`.

**Implementation:**

Remove the square-mode sizing branch from `FullScreenVisualizer`. Render `TerminalVisualizerScene` with `Modifier.fillMaxSize()` for every full-screen audio mode.

Do not change the internal Radar or Grid geometry in this task. Radar will remain circular because `radarGeometry()` already uses `min(width, height)`; Grid will still draw its old centered square until later tasks.

Do not touch swipe, Exit, timer, safe-inset, or system-bar behavior.

**Verify:**

Run:

`.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.visualizer.FullScreenVisualizerTest`

The new sizing test passes and the existing interaction tests remain green.

---

### Task 2: Add an explicit expanded rendering context

**Files:**  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizer.kt`  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`

**Test first:**

Add `fullScreenUsesExpandedSceneWhileNormalCallersDefaultToStandard` to `VisualizerSourceGuardTest`.

Assert that:

- `TerminalVisualizerScene` declares an `expanded: Boolean = false` parameter.
- `FullScreenVisualizer.kt` passes `expanded = true`.
- No existing caller is required to pass the parameter because the default remains `false`.

This is a source-contract test only; do not change visual behavior yet.

**Implementation:**

Add `expanded: Boolean = false` to `TerminalVisualizerScene` and `TerminalVisualizerFrame`.

Forward it from scene to frame.

In `FullScreenVisualizer`, pass `expanded = true`.

All existing NOW callers continue using the default `false`.

Do not create a second visualizer composable or duplicate audio/afterglow state.

**Verify:**

Run:

`.\gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ui.playback.visualizer.VisualizerSourceGuardTest"`

All source-guard tests pass.

---

### Task 3: Define portrait Grid geometry

**Files:**  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Add focused tests for a new `frequencyGridPortraitGeometry(...)` function:

1. `portraitGridKeepsThirtyColumnsAndAddsRowsForTallViewports`
   - For 300×600, assert 30 columns.
   - Assert row count is greater than 30 and is derived from the available height.
   - Assert every cell has the same width/height represented by the existing square `size` field.
   - Assert all cells remain within the viewport.

2. `portraitGridCentersSmallRemainderWithoutStretchingCells`
   - Use a viewport whose height is not an exact multiple of the horizontal pitch.
   - Assert the top and bottom unused margins differ by no more than one pixel-equivalent tolerance.
   - Assert cell size is unchanged across all rows.

3. `portraitGridIsDeterministicAndSafeForInvalidSizes`
   - Same inputs return exactly equal cell lists.
   - Zero, negative, NaN, and infinite dimensions return an empty list rather than malformed geometry.

Keep the existing `frequencyGridGeometry` 30×30 tests unchanged; they protect NOW behavior.

**Implementation:**

Add a new `frequencyGridPortraitGeometry(liveLevels, afterglow, width, height)`.

Use these rules:

- Fixed horizontal dimension: `FREQUENCY_GRID_DIMENSION` (30 columns).
- Derive pitch from usable width exactly as the current square Grid does.
- Keep the current gap ratio and maximum inset behavior.
- Calculate the largest whole row count that fits the usable height at that pitch.
- Center the resulting whole-row field vertically so any remainder is split between top and bottom.
- Return one `FrequencyGridCell` per row/column.
- Reuse the existing live/ghost sanitization and blending behavior rather than duplicating it.

Do not alter `frequencyGridGeometry`; it remains the standard square layout.

**Verify:**

Run:

`.\gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ui.playback.visualizer.VisualizerGeometryTest"`

All old and new geometry tests pass.

---

### Task 4: Add deterministic portrait Grid hotspot placement

**Files:**  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Add `portraitFrequencyAnchorsCoverTheTallFieldWithoutFrequencyStripes`.

For each of the 32 bands independently:

- Render portrait geometry at 300×600 with exactly one active band.
- Find that band's peak cell.
- Assert all 32 peak positions are unique.
- Assert peaks occur in upper, middle, and lower thirds.
- Assert peaks occur on both left and right halves.
- Assert the sequence of band indexes is not monotonically ordered by Y; low-to-high frequency must not form rigid horizontal frequency stripes.
- Re-running the same inputs returns the same peak locations.

**Implementation:**

Add a dedicated fixed `frequencyGridPortraitAnchors` list containing 32 normalized points distributed across the full portrait field.

The list must:

- span approximately the full normalized Y range while leaving a small edge inset;
- use both halves of the X range throughout the height;
- deliberately interleave band indexes vertically rather than ordering them low-to-high;
- remain fixed in source so resize/redraw never randomizes the pattern.

Refactor the hotspot blending helper to accept the anchor list as an argument.

- `frequencyGridGeometry` passes the existing square anchors.
- `frequencyGridPortraitGeometry` passes the new portrait anchors.

Keep the hotspot radius, blending formula, clamping, live intensity, and ghost intensity rules shared.

**Verify:**

Run the same focused `VisualizerGeometryTest` command. Existing square-anchor tests and new portrait-anchor tests all pass.

---

### Task 5: Render portrait Grid only when expanded

**Files:**  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`  
`app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizerTest.kt`

**Test first:**

Add two checks:

1. In `VisualizerSourceGuardTest`, add `expandedGridUsesPortraitGeometryAndStandardGridKeepsSquareGeometry`.
   - Assert the GRID branch selects `frequencyGridPortraitGeometry` only when `expanded` is true.
   - Assert `frequencyGridGeometry` remains the standard/default path.

2. In `FullScreenVisualizerTest`, add `gridSceneOccupiesTheTallFullScreenViewport`.
   - Render GRID in a 240×480 host.
   - Assert the `visualizer-grid` node fills the full root bounds.
   - This complements the pure geometry tests; it does not attempt to inspect individual Canvas cells through semantics.

**Implementation:**

In the GRID branch of `TerminalVisualizerFrame`:

- call `frequencyGridPortraitGeometry` when `expanded == true`;
- otherwise call the existing `frequencyGridGeometry`.

Leave the existing drawing order exactly intact:

1. base cells;
2. ghost intensity;
3. live intensity;
4. peak overlay.

Do not change colors, intensity curves, afterglow, or scanlines.

**Verify:**

Run `VisualizerGeometryTest`, `VisualizerSourceGuardTest`, and `FullScreenVisualizerTest`.

---

### Task 6: Add full-viewport Radar ray geometry

**Files:**  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Add `radarViewportEndpointHitsTheCorrectScreenEdge` for a helper named `radarViewportEndpoint`.

Using a 240×480 viewport centered at (120, 240), verify representative angles:

- 0° reaches the top edge;
- 90° reaches the right edge;
- 180° reaches the bottom edge;
- 270° reaches the left edge;
- diagonal angles terminate on one of the rectangle edges;
- returned points are finite and remain within the viewport bounds;
- equivalent normalized angles such as 0°/360° and -2°/358° return equal endpoints.

Also add a tall-canvas assertion to the existing Radar geometry test proving the circular core remains centered and uses the shorter viewport dimension.

**Implementation:**

Add `radarViewportEndpoint(center, width, height, sweepDegrees)`.

Normalize the angle using the same convention as `radarSweepEndpoint` (0° points upward), cast a ray from the center, calculate the positive intersection distance with the rectangle's vertical and horizontal boundaries, and return the nearest valid intersection.

Handle non-finite/zero dimensions safely by returning the center rather than propagating NaN/Infinity.

Do not modify `radarGeometry` or `radarSweepEndpoint`.

**Verify:**

Run the focused `VisualizerGeometryTest` command.

---

### Task 7: Add pure full-screen Radar pulse state

**Files:**  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenRadarEffectsTest.kt`  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenRadarEffects.kt`

**Test first:**

Create `FullScreenRadarEffectsTest` covering:

1. `risingTransientCreatesOnePulse`
   - A positive finite transient that rises above the state's previous transient creates one pulse.
   - Re-processing the same/decaying transient does not create another pulse.

2. `pulseProgressExpandsAndFadesMonotonically`
   - Advancing elapsed time increases normalized progress/radius.
   - Alpha decreases with age.
   - Strength is clamped to 0..1.

3. `expiredPulsesAreRemoved`
   - Advancing past the pulse lifetime removes the pulse.

4. `closeTransientsRemainBounded`
   - Repeated rising transients never keep more than a small fixed maximum number of pulses.

5. `invalidTransientDoesNotCreatePulse`
   - NaN, infinities, and non-positive values do not create a new pulse.

**Implementation:**

Create a small pure model:

- `RadarFullScreenPulse(strength, ageNanos)`;
- `RadarFullScreenEffectState(previousTransient, pulses)`;
- `updateRadarFullScreenEffects(state, transient, elapsedNanos)`.

Rules:

- Age existing pulses by non-negative elapsed time.
- Remove expired pulses.
- Spawn a new pulse only when the sanitized current transient is greater than the previous sanitized transient; this uses the analyzer's existing attack/decay signal without adding a new beat detector.
- Capture the sanitized transient as pulse strength.
- Keep only the newest bounded number of pulses.
- Expose pure helpers for normalized pulse progress and alpha so rendering tests do not depend on Canvas internals.

Use constants in this file for pulse lifetime, maximum pulse count, and maximum pulse alpha; do not add settings or persistence.

**Verify:**

Run:

`.\gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ui.playback.visualizer.FullScreenRadarEffectsTest"`

All pulse-state tests pass.

---

### Task 8: Lock the full-screen Radar intensity hierarchy

**Files:**  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerRenderingStyleTest.kt`  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenRadarEffects.kt`

**Test first:**

Add `fullScreenRadarEffectsRemainSubordinateToTheCore`.

Introduce named constants for:

- extended live beam alpha;
- extended live beam width;
- extended wake alpha scale;
- extended wake width;
- pulse leading-edge maximum alpha;
- pulse body maximum alpha.

Assert:

- wake alpha is lower than extended beam alpha;
- extended beam alpha is lower than the existing crisp core sweep alpha (.90);
- pulse body alpha is lower than pulse leading-edge alpha;
- pulse leading-edge alpha remains below the crisp core sweep;
- all values are positive and restrained.

Do not test exact visual taste beyond the hierarchy unless an exact value is required for deterministic rendering.

**Implementation:**

Define the constants in `FullScreenRadarEffects.kt`.

Use conservative starting values consistent with the existing phosphor hierarchy. Keep them centralized so physical-device tuning later changes constants rather than geometry/state logic.

Do not alter the existing normal Radar bloom constants.

**Verify:**

Run:

`.\gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ui.playback.visualizer.VisualizerRenderingStyleTest"`

All style hierarchy tests pass.

---

### Task 9: Drive Radar pulses from the existing Compose frame clock

**Files:**  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`

**Test first:**

Add `fullScreenRadarPulseStateUsesTheExistingFrameClockAndEligibility` to `VisualizerSourceGuardTest`.

Assert that:

- `TerminalVisualizerScene` owns remembered `RadarFullScreenEffectState`;
- it updates that state inside the existing `withFrameNanos` loop rather than creating a second timer/animation loop;
- pulse state is eligible only when `expanded`, mode is RADAR, effects are enabled, analysis is ACTIVE, and measured size is valid;
- ineligible states reset to the empty/default pulse state;
- the source still contains none of the forbidden wall-clock/timer APIs already guarded by the test.

**Implementation:**

In `TerminalVisualizerScene`:

- remember one `RadarFullScreenEffectState`;
- inside the existing frame-clock loop, update it with `latestFrame.transient` and the same `elapsedNanos` used for afterglow when full-screen Radar is eligible;
- reset it immediately when eligibility is lost;
- pass the current state to `TerminalVisualizerFrame`.

Do not create another `LaunchedEffect`, delay loop, infinite transition, or persisted state.

Normal/NOW rendering must keep this state empty because `expanded` defaults to false.

**Verify:**

Run `VisualizerSourceGuardTest` and `FullScreenRadarEffectsTest`.

---

### Task 10: Draw the full-screen Radar pulse, wake, and extended beam

**Files:**  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`

**Test first:**

Add `expandedRadarDrawsOuterEffectsBeforeTheExistingCore` to `VisualizerSourceGuardTest`.

Within the RADAR branch, assert the expanded-only calls occur in this order:

1. full-screen transient pulse;
2. extended afterglow wake;
3. extended live beam;
4. existing `drawRadarBloom` when effects are enabled;
5. existing `drawRadarCore`.

Also assert the outer-effect calls are guarded by `expanded && effectsEnabled`, so NOW Radar never renders them.

**Implementation:**

Add focused private `DrawScope` helpers in `TerminalVisualizers.kt`:

- `drawRadarFullScreenPulses(...)`
- `drawRadarExtendedWake(...)`
- `drawRadarExtendedBeam(...)`

Pulse rendering:

- center at `RadarGeometry.center`;
- compute maximum travel radius as the distance from center to the furthest viewport corner;
- convert each pulse's normalized progress into a current radius;
- draw a broader low-alpha body and a narrower/brighter leading ring using the existing theme visualizer color;
- multiply alpha by captured pulse strength and age fade;
- let rings naturally move through the circular radar area and beyond it.

Extended wake:

- reuse the existing `afterglow.radar.samples`;
- convert each sample angle to `radarViewportEndpoint`;
- draw center-to-edge lines using the sample's existing decay alpha scaled by the new, lower full-screen wake alpha constant;
- use the broader wake width.

Extended live beam:

- use `geometry.sweepDegrees`;
- draw center-to-`radarViewportEndpoint` with the restrained beam alpha/width.

Then draw the existing Radar bloom/core unchanged above those layers.

Do not add blur APIs or shaders.

**Verify:**

Run `VisualizerSourceGuardTest`, `VisualizerRenderingStyleTest`, `VisualizerGeometryTest`, and `FullScreenRadarEffectsTest`.

---

### Task 11: Remove the old square-margin ambient glow

**Files:**  
Delete `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAmbientGlow.kt`  
Delete `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAmbientGlowTest.kt`  
Update `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizer.kt`  
Update `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizerTest.kt`  
Update `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`

**Test first:**

Replace `glowAppearsOnlyForActiveSquareModesWithEffects` with `legacyMarginGlowIsAbsent`.

Assert `full-screen-glow` does not exist for RADAR, GRID, or BANDS.

Add a source-guard assertion that `ambientGlowAlpha` and `full-screen-glow` are absent from production visualizer source.

The pure Radar pulse tests from Tasks 7–10 now cover beat-effect eligibility and decay.

**Implementation:**

Remove from `FullScreenVisualizer`:

- `ambientGlowAlpha(...)`;
- the top/bottom gradient Canvas;
- the `full-screen-glow` tag;
- now-unused Brush/Offset/Size imports.

Delete `VisualizerAmbientGlow.kt` and its obsolete unit test.

Do not replace Grid's old margin glow with a new Grid-specific glow; the approved redesign gives Grid the portrait field itself.

**Verify:**

Run the focused unit tests plus `FullScreenVisualizerTest`.

---

### Task 12: Add deterministic rendering coverage for Radar outside the core

**Files:**  
`app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizerTest.kt`

**Test first:**

Add a rendering test named `activeExpandedRadarDrawsBeyondTheCircularCoreOnlyWhenEffectsAreEnabled`.

Use a 240×480 host and an ACTIVE synthetic frame with a nonzero transient.

With effects enabled:

- advance the Compose clock/frame enough for the expanded scene to receive the frame and create/age a pulse;
- capture the `visualizer-radar` image;
- sample at least one point above or below the circular core but still well inside the viewport;
- assert that pixel differs from the plain terminal background.

Then render the same frame with effects disabled and assert that the same outside-core sample matches the background within channel tolerance.

Choose sample locations away from scanline boundaries and the Exit control area. Keep the assertion tolerant enough for theme alpha compositing but strict enough to prove expanded Radar draws outside its core.

**Implementation:**

No production behavior should be added in this task. If this test exposes nondeterminism, make the smallest rendering/state correction needed without introducing delays or a second animation mechanism.

**Verify:**

Run `FullScreenVisualizerTest` repeatedly on the same emulator/device to confirm deterministic results.

---

### Task 13: Confirm Grid portrait rendering and Spectrum regression

**Files:**  
`app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenVisualizerTest.kt`  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`

**Test first:**

Add/retain assertions that:

- full-screen GRID fills the 240×480 scene bounds;
- portrait Grid geometry has cells in both the upper and lower quarters of the viewport, proving it is not still a centered square;
- BANDS continues to fill the full viewport;
- standard `frequencyGridGeometry` still returns exactly 900 cells and remains centered as before;
- `radarGeometry` on a tall canvas still creates a circular core based on the shorter dimension.

**Implementation:**

No new production behavior should be required. If a regression exists, fix only the relevant expanded/standard branch selection.

**Verify:**

Run `VisualizerGeometryTest` and `FullScreenVisualizerTest`.

---

### Task 14: Run full local regression and inspect the diff

**Files:** No source changes expected.

**Test first / verification sequence:**

Run:

`.\gradlew.bat :app:testDebugUnitTest`

`.\gradlew.bat :app:connectedDebugAndroidTest`

`.\gradlew.bat :app:validateDebugScreenshotTest`

`.\gradlew.bat :app:assembleDebug`

Then run:

`git diff --check`

`git status --short`

**Implementation:**

Fix only failures caused by this planned feature. Do not refactor unrelated visualizer, playback, or navigation code.

Confirm the modified file set is limited to the files named in this plan plus the approved design/plan documents.

**Verify:**

All local automated checks pass and the debug APK builds.

---

### Task 15: Physical Pixel verification and one CI batch

**Files:** No source changes expected unless physical testing exposes a defect in the approved behavior.

**Test first / manual verification:**

On the physical Pixel, verify:

**Radar**
- circular core remains centered and recognizable;
- no visible opaque square boundary remains;
- faint extended beam reaches the display boundary as the sweep rotates;
- wider wake trails behind the beam and remains dimmer;
- quiet passages do not leave stale pulses;
- strong transients launch visible waves from the radar center;
- waves pass through the core and continue toward edges/corners;
- repeated beats do not create an overwhelming pile-up;
- reduced-motion/effects-disabled behavior suppresses the optional expanded effects.

**Grid**
- field visibly extends through the portrait screen rather than occupying a centered square;
- cells remain square;
- activity appears across upper, middle, and lower regions;
- sparse and dense material both remain readable;
- no obvious horizontal low/mid/high striping appears.

**Regression**
- Spectrum is unchanged;
- swipe navigation still cycles Radar → Spectrum → Grid;
- tap/Exit timer still works;
- Android Back and immersive-system-bar restoration still work;
- returning to NOW shows the unchanged standard visualizers.

**Implementation:**

If visual tuning is needed, adjust only the centralized full-screen Radar rendering constants or portrait-anchor coordinates. Do not change audio analysis or add settings.

Once physical verification is satisfactory, push the single completed TDD batch.

Actively monitor the resulting GitHub Actions run during the same implementation response. Re-check until it completes; if CI fails, inspect the failure, make the smallest correction, push, and continue monitoring until green or until a genuine user-input blocker is reached.

**Verify:**

Physical-device behavior matches the approved design and GitHub Actions is green.

## Definition of Done

- [ ] All tasks completed sequentially with each behavioral test written before its implementation.
- [ ] Full-screen Grid uses a portrait-native square-cell field with roughly 30 columns and deterministic full-height hotspot placement.
- [ ] Standard/NOW Grid remains the original centered 30×30 visualization.
- [ ] Full-screen Radar keeps the existing centered circular core while the faint beam/wake extend to the viewport boundary.
- [ ] Radar transient pulses originate at the center, expand through the core toward the edges/corners, fade, expire, and remain bounded.
- [ ] The old top/bottom margin glow and `VisualizerAmbientGlow` helper are removed.
- [ ] NOW Radar/Grid and full-screen Spectrum remain behaviorally unchanged.
- [ ] Existing swipe, Exit, Back, immersive-system-bar, sync, reduced-motion, and audio-analysis behavior remains intact.
- [ ] Unit, instrumentation, screenshot-validation, and assemble tasks all pass.
- [ ] Physical Pixel verification passes.
- [ ] Only planned files are modified.
- [ ] The completed batch is pushed once and GitHub Actions is monitored to green.
