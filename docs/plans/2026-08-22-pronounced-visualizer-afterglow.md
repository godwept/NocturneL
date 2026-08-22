# Pronounced Visualizer Afterglow Implementation Plan

**Date:** 2026-08-22  
**Design doc:** `docs/specs/2026-08-22-pronounced-visualizer-afterglow-design.md`  
**Status:** Ready for review

## Overview

Make the existing radar and spectrum afterglows unmistakable without changing visualizer controls, layout, audio analysis, or public composable interfaces. Both modes will use a 500 ms monotonic decay; radar will retain a longer bounded sweep history and render tiered wide-stroke canvas bloom beneath crisp geometry, while bands will render brighter falling ghost segments beneath live bars.

## Fixed Implementation Decisions

- Keep `TerminalVisualizerScene(mode, frame, effectsEnabled, modifier)` and `TerminalVisualizerFrame(...)` signatures unchanged.
- Change the shared decay constant to `AFTERGLOW_DURATION_NANOS = 500_000_000L`.
- Increase bounded radar history from 8 to 16 samples so the longer decay retains comparable angular density without unbounded growth.
- Set `RADAR_AFTERGLOW_MAX_ALPHA = .70f` and `BAND_AFTERGLOW_MAX_ALPHA = .72f`. Retained cores remain below the live sweep alpha `.90f` and live bar alpha `1f`.
- Render band ghosts with `Phosphor.copy(alpha = envelope.alpha)` instead of `PhosphorDim`; keep their existing geometry and render them before live bars.
- Add no real blur, shader, bitmap layer, dependency, persisted setting, or new public API. Simulate phosphor bloom with wide translucent Canvas strokes beneath existing crisp strokes.
- Use these internal radar bloom constants:
  - grid: alpha `.16f`, width `4f`;
  - energy rings: alpha `.22f`, width `5f`;
  - spokes: alpha `.20f`, width `4f`;
  - transient echo: maximum alpha `.28f`, width `6f`;
  - retained sweep arms: retained alpha multiplied by `.45f`, width `5f`;
  - live sweep: alpha `.36f`, width `7f`.
- Preserve the existing crisp grid, energy-ring, spoke, and echo styling. Draw retained sweep cores with `Phosphor.copy(alpha = sample.alpha)` at `1.5f`, and draw the live sweep core with `PhosphorBright.copy(alpha = .90f)` at `1.5f`.
- Draw all bloom geometry first, then every crisp core element. Draw retained history oldest-to-newest and the live sweep last within each pass.
- Preserve all existing lifecycle clearing rules, frame-clock timing, geometry, tags, scanlines, semantics, and unavailable-state behavior.

## Tasks

### Task 1: Specify the 500 ms bounded decay (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAfterglowTest.kt`

**Test first:**

Update the existing duration, capacity, and visibility contracts before changing production constants:

```kotlin
@Test fun afterglowIsPronouncedButSubordinateToLiveElements() {
    assertEquals(500_000_000L, AFTERGLOW_DURATION_NANOS)
    assertEquals(.70f, RADAR_AFTERGLOW_MAX_ALPHA, 0f)
    assertEquals(.72f, BAND_AFTERGLOW_MAX_ALPHA, 0f)
    assertTrue(RADAR_AFTERGLOW_MAX_ALPHA < .90f)
    assertTrue(BAND_AFTERGLOW_MAX_ALPHA < 1f)
}

@Test fun newRadarFramesRetainAtMostSixteenPriorAngles()
@Test fun radarTrailFadesMonotonicallyAndExpiresAt500Milliseconds()
@Test fun bandGhostRemainsBeforeAndExpiresAt500Milliseconds()
```

For the capacity case, feed 20 increasing frame IDs and assert the retained angles are the newest 16. For radar decay, sample at 0, 250, 499, and 500 ms and assert strictly decreasing alpha followed by empty history. For a falling band, assert retained height and alpha remain above baseline at 499 ms and collapse to the live baseline at 500 ms. Confirm these tests fail against the current 250 ms/eight-sample implementation.

**Implementation:** None in this task; preserve the red tests as the contract for Task 2.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerAfterglowTest'
```

The focused suite fails only on the newly updated duration, intensity, and capacity expectations.

### Task 2: Implement the longer, brighter decay (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAfterglow.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAfterglowTest.kt`

**Test first:** Use the failing tests from Task 1; do not weaken their exact 500 ms, 16-sample, or alpha expectations.

**Implementation:**

- Change `AFTERGLOW_DURATION_NANOS` from `250_000_000L` to `500_000_000L`.
- Change `RADAR_AFTERGLOW_MAX_ALPHA` to `.70f` and `BAND_AFTERGLOW_MAX_ALPHA` to `.72f`.
- Change `MAX_RADAR_AFTERGLOW_SAMPLES` from `8` to `16`.
- Keep the existing linear `decayAlpha`, monotonic elapsed-time handling, retained-level descent, sanitization, and lifecycle reset logic unchanged.
- Rename the affected test functions from “250Milliseconds” and “Eight” to their 500 ms/16-sample forms, and change intermediate band assertions to use 250 ms as the halfway point rather than the expiry point.

**Verify:** Run the focused `VisualizerAfterglowTest`. All state, malformed-input, lifecycle, and new intensity tests pass.

### Task 3: Specify the radar bloom hierarchy (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerRenderingStyleTest.kt`

**Test first:**

Create a focused JVM test for the internal rendering constants:

```kotlin
class VisualizerRenderingStyleTest {
    @Test fun radarBloomUsesApprovedWidthsAndOpacityTiers() {
        assertEquals(.16f, RADAR_GRID_BLOOM_ALPHA, 0f)
        assertEquals(4f, RADAR_GRID_BLOOM_WIDTH, 0f)
        assertEquals(.22f, RADAR_ENERGY_BLOOM_ALPHA, 0f)
        assertEquals(5f, RADAR_ENERGY_BLOOM_WIDTH, 0f)
        assertEquals(.20f, RADAR_SPOKE_BLOOM_ALPHA, 0f)
        assertEquals(4f, RADAR_SPOKE_BLOOM_WIDTH, 0f)
        assertEquals(.28f, RADAR_ECHO_BLOOM_MAX_ALPHA, 0f)
        assertEquals(6f, RADAR_ECHO_BLOOM_WIDTH, 0f)
        assertEquals(.45f, RADAR_TRAIL_BLOOM_ALPHA_SCALE, 0f)
        assertEquals(5f, RADAR_TRAIL_BLOOM_WIDTH, 0f)
        assertEquals(.36f, RADAR_SWEEP_BLOOM_ALPHA, 0f)
        assertEquals(7f, RADAR_SWEEP_BLOOM_WIDTH, 0f)
    }

    @Test fun movingBloomDominatesStaticBloomWithoutExceedingCores() {
        assertTrue(RADAR_GRID_BLOOM_ALPHA < RADAR_ENERGY_BLOOM_ALPHA)
        assertTrue(RADAR_SPOKE_BLOOM_ALPHA < RADAR_SWEEP_BLOOM_ALPHA)
        assertTrue(RADAR_ECHO_BLOOM_MAX_ALPHA < RADAR_SWEEP_BLOOM_ALPHA)
        assertTrue(RADAR_SWEEP_BLOOM_ALPHA < .90f)
        assertTrue(RADAR_AFTERGLOW_MAX_ALPHA * RADAR_TRAIL_BLOOM_ALPHA_SCALE < RADAR_AFTERGLOW_MAX_ALPHA)
    }
}
```

Confirm the test fails because the constants do not exist.

**Implementation:** None in this task; Task 4 introduces the constants and rendering passes.

**Verify:** Run `testDebugUnitTest --tests '*VisualizerRenderingStyleTest'`; compilation fails only for the missing approved constants.

### Task 4: Add complete radar bloom and crisp-core passes (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerRenderingStyleTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`

**Test first:**

In `VisualizerSourceGuardTest`, add a contract that isolates the RADAR branch between `VisualizerDisplayMode.RADAR ->` and `VisualizerDisplayMode.BANDS ->`, then asserts:

```kotlin
assertTrue(radarBranch.indexOf("drawRadarBloom(") < radarBranch.indexOf("drawRadarCore("))
```

Also assert the full source contains neither `RenderEffect` nor `BlurEffect`. Run both new tests and confirm the style test does not compile and the source-order test fails.

**Implementation:**

- Declare the twelve exact internal constants from Task 3 near the top of `TerminalVisualizers.kt`.
- Add private `DrawScope.drawRadarBloom(geometry, frame, samples)` and `DrawScope.drawRadarCore(geometry, frame, samples)` helpers.
- In `drawRadarBloom`, draw grid circles, energy rings, spokes, the conditional transient echo, retained arms oldest-to-newest, and the live sweep using the approved wide widths and alpha tiers. Multiply echo alpha by `frame.transient.coerceIn(0f, 1f)` and multiply each retained sample's alpha by `.45f`.
- In `drawRadarCore`, move the current grid, energy-ring, spoke, and echo drawing without changing their established styles. Draw retained arms in `Phosphor.copy(alpha = sample.alpha)` at `1.5f`, followed by the live `PhosphorBright` arm at alpha `.90f` and width `1.5f`.
- Reuse `radarSweepEndpoint` in both helpers. Do not duplicate or change geometry calculations.
- Replace the inline RADAR drawing block with `drawRadarBloom(...)` followed immediately by `drawRadarCore(...)`. Compose Canvas clips both passes to its bounds by default; add no offscreen bitmap or graphics layer.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerRenderingStyleTest' --tests '*VisualizerSourceGuardTest' --tests '*VisualizerGeometryTest'
.\gradlew.bat assembleDebug
```

The hierarchy, no-platform-blur, geometry, and compilation checks pass.

### Task 5: Make band ghosts unmistakable (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`

**Test first:**

Add a source contract that isolates the BANDS branch and asserts:

- `spectrumGhostGeometry(` appears before `spectrumGeometry(`;
- ghost rectangles use `Phosphor.copy(alpha = afterglow.bands[ghost.bandIndex].alpha)`;
- the branch does not use `PhosphorDim.copy(alpha = afterglow.bands`;
- live rectangles still use `Phosphor` and the peak marker still uses `PhosphorBright` after ghost drawing.

Run `VisualizerSourceGuardTest` and confirm it fails because ghosts still use `PhosphorDim`.

**Implementation:** Change only the ghost rectangle color from `PhosphorDim.copy(...)` to `Phosphor.copy(...)`. Preserve segment dimensions, ghost-only-above-live geometry, draw order, live bar styling, and peak-marker styling.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerSourceGuardTest' --tests '*VisualizerGeometryTest' --tests '*VisualizerAfterglowTest'
```

All band geometry, brightness, duration, and ordering contracts pass.

### Task 6: Update lifecycle coverage for the longer effect (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizersTest.kt`

**Test first:**

In `activeSceneClearsEffectsAndHistoryAcrossLifecycleChanges`, replace each post-reset `advanceTimeBy(300)` with `advanceTimeBy(550)`. Before implementation verification, confirm the test still covers effects-off clearing, mode switching, unavailable analysis, restoration, stable tags, and absent click actions over a period longer than the new decay.

**Implementation:** No visualizer production change is expected. If the updated test exposes a regression, correct only the existing immediate-reset behavior in `TerminalVisualizerScene`; do not add semantics or test tags for Canvas history.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ca.stewark.nocturnel.ui.playback.visualizer.TerminalVisualizersTest
```

If no emulator or device is attached, Android-test assembly must pass and the connected run is reported as pending.

### Task 7: Refresh deterministic afterglow screenshots (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/VisualizerRadarAfterglowPreview_Visualizer radar afterglow_64f04abc_0.png`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/VisualizerBandsAfterglowPreview_Visualizer bands afterglow_e1ba8762_0.png`

**Test first:** Run `validateDebugScreenshotTest` after Tasks 2–5 and confirm the two named afterglow references fail while unrelated screenshot references remain unchanged.

**Implementation:**

- In `VisualizerRadarAfterglowPreview`, change retained sample ages to `360_000_000`, `240_000_000`, and `120_000_000` nanoseconds so the fixture visibly represents the full 500 ms gradient.
- In `VisualizerBandsAfterglowPreview`, change the patterned retained lift from `.35f` to `.45f` and the explicit age from `80_000_000` to `160_000_000` nanoseconds.
- Run `updateDebugScreenshotTest` and review the two generated images. The radar must show a broad glow around every radar element with crisp geometry over it and dominant motion bloom. Bands must show bright, obvious ghost segments only above live bars.
- Keep no changes to unrelated reference images.

**Verify:** Run:

```powershell
.\gradlew.bat updateDebugScreenshotTest
.\gradlew.bat validateDebugScreenshotTest
git status --short -- app/src/screenshotTest app/src/screenshotTestDebug/reference
```

Only the preview source and the two named reference PNGs change.

### Task 8: Update the device acceptance contract (2–5 min)

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:** Search the visualizer checklist and confirm it still requires a “slight” sweep-only trail, crisp non-sweep radar geometry, and approximately 250 ms band ghosts; these statements now contradict the approved design.

**Implementation:** Replace only those stale afterglow items so the checklist requires:

- pronounced but readable bloom on the complete radar, including grid, energy rings, spokes, transient echo, live sweep, and retained trail;
- crisp cores that keep the radar geometry readable above the bloom;
- a sweep trail and falling band ghosts that persist for about 500 ms;
- band ghosts that are clearly noticeable but remain behind and dimmer than live segments and peak markers;
- the existing immediate-clear and 30-minute playback/performance checks without alteration.

**Verify:** Run:

```powershell
rg -n '250 ms|slight trail|rings, spokes|500 ms|bloom' docs/testing/pixel-7-release-checklist.md
```

No stale 250 ms or sweep-only wording remains, and the new approved expectations are present.

### Task 9: Run automated regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–8

**Test first:** Run the focused source scan and diff checks:

```powershell
rg -n 'RenderEffect|BlurEffect|graphicsLayer|System\.currentTimeMillis|System\.nanoTime|elapsedRealtime|rememberInfiniteTransition|infiniteRepeatable|delay\(' app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer
git diff --check
```

The source scan returns no matches and the diff check reports no errors.

**Implementation:** Fix only failures introduced by this approved change. Do not alter audio analysis, FFT processing, synchronization, settings, visualizer mode ordering, layout, controls, dependencies, theme colors, or unrelated screenshots. Preserve all pre-existing user changes in the worktree.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebugAndroidTest
.\gradlew.bat validateDebugScreenshotTest
.\gradlew.bat lintRelease
.\gradlew.bat assembleDebug
git diff --check
git status --short
```

If a device or emulator is attached, also run `connectedDebugAndroidTest`. Explicitly report the connected suite as pending if suitable hardware is unavailable.

### Task 10: Perform the perceptual device spot-check (2–5 min)

**Files:** no source files; use `docs/testing/pixel-7-release-checklist.md` as the acceptance contract

**Test first:** On the target phone at ordinary brightness, open Now Playing with CRT effects enabled and play a percussion-heavy or dynamically varied passage. Before making any further tuning change, observe radar and bands against the approved checklist wording.

**Implementation:** If the effect misses the approved target, tune only the fixed alpha and width constants named in this plan, regenerate only the two afterglow references, and rerun the focused style/screenshot checks. Do not add a setting or change decay/state behavior during perceptual tuning.

**Verify:** During the passage, confirm complete-radar bloom is pronounced but readable, sweep and band ghosts remain visible for about 500 ms, live geometry stays dominant, and pause/mode/effects changes clear history immediately. Record this spot-check as pending if suitable hardware and audio are unavailable; retain the existing 30-minute performance item for the normal release checklist rather than extending this implementation task.

## Definition of Done

- [ ] All tasks completed in order with each behavioral change preceded by its failing test or contract check.
- [ ] Radar and band persistence uses a shared 500 ms monotonic decay.
- [ ] Radar retains no more than 16 historical sweep samples.
- [ ] Complete radar geometry has tiered wide-stroke bloom beneath crisp readable cores.
- [ ] Radar motion bloom is stronger than static bloom, and the live sweep remains dominant over retained arms.
- [ ] Falling band ghosts are clearly visible, cover only retained segments above live bars, and remain subordinate to live bars and peak markers.
- [ ] ART, effects off, IDLE, UNAVAILABLE, mode changes, size changes, band-count changes, frame rewinds, and large time gaps clear history safely.
- [ ] `TerminalVisualizerScene` and `TerminalVisualizerFrame` retain their existing interfaces, tags, dimensions, scanlines, semantics, and unavailable fallback.
- [ ] No platform blur, shader, bitmap layer, dependency, setting, audio, synchronization, layout, control, or palette change is introduced.
- [ ] Unit tests, Android-test assembly, screenshot validation, release lint, and debug assembly pass.
- [ ] Connected tests and device visual/performance checks pass or are explicitly reported as pending.
- [ ] Only planned files and the approved design/plan are modified.
