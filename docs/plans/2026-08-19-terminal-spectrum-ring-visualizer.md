# Terminal Spectrum Ring Visualizer Implementation Plan

**Date:** 2026-08-19  
**Design doc:** `docs/specs/2026-08-19-terminal-spectrum-ring-visualizer-design.md`  
**Status:** Ready for review

## Overview

Replace the fourth visualizer mode's kaleidoscope tunnel with a tilted, phosphor-green radial spectrum ring. The implementation will rename the mode, project synchronized analysis frames into bounded perspective-aware spike geometry, add one-frame temporal smoothing and a short transient echo, render it with the existing CRT palette, replace tunnel tests and screenshots, and preserve all analyzer, playback, and shared sync-offset behavior.

## Fixed Implementation Decisions

- Rename `VisualizerDisplayMode.TUNNEL` to `RING`, use label `RING 4/4`, accessibility name `Terminal spectrum ring`, and scene tag `visualizer-ring`; retain the fourth position in the cycle.
- Use 64 spikes when the smaller canvas dimension is below 160 px, 80 below 280 px, and 96 otherwise.
- Center the ellipse at `(width / 2, height / 2)`. Use a calm horizontal radius of 28% of safe width and vertical radius of 15% of safe height, leaving room for outward spikes.
- Allow bass to increase both radii by at most 8%. Clamp spike tips and echo points to a 4% canvas safety inset.
- Calculate the projected outward normal by normalizing `(cos(angle) / horizontalRadius, sin(angle) / verticalRadius)`, the gradient of the ellipse equation. Do not point every spike directly away from the canvas center.
- Treat the lower half as the near side. Define normalized depth as `(sin(angle) + 1f) / 2f`; use it only for draw order and stroke/color emphasis, not to move the ring center.
- Derive orbit phase as `floorMod(frameId, 1_440) / 1_440f * 2π`. At approximately 30 analysis frames per second this completes one slow orbit in about 48 seconds, freezes on an unchanged frame, and uses no timer.
- Resample absolute waveform magnitude around the ring after applying the orbit phase. Smooth adjacent magnitudes with weights `.25f / .50f / .25f`, then blend consecutive active frames as `.35f * previous + .65f * current`.
- Give every spike a calm baseline of 1.5% of the smaller canvas dimension. Add up to 13% from waveform magnitude shaped by mid energy and up to 4% of deterministic fine tip detail shaped by high energy. Cap total spike length at 18.5%.
- Start an echo only when sanitized transient is at least `.65f`. Retain one echo for four increasing active frame IDs; expand it by 3%, 6%, 9%, and 12% while fading it. Effects-disabled state retains smoothing but never starts or retains an echo.
- Draw spikes far-to-near. Use `PhosphorMuted` for the far third, `Phosphor` for the middle third, and `PhosphorBright` for the near third, with stroke widths from `0.8f` to `2f`. Effects enabled adds one dim under-stroke 3 px wider; it does not add historical spike trails.
- Draw a faint closed base ellipse behind the spikes. Draw the optional expanding echo ellipse last. Use only the existing terminal colors.

## Tasks

### Task 1: Rename the fourth display mode (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDisplayModeTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDisplayMode.kt`

**Test first:**

Change `VisualizerDisplayModeTest` to require:

```kotlin
assertEquals(VisualizerDisplayMode.RING, VisualizerDisplayMode.BANDS.next())
assertEquals(VisualizerDisplayMode.ART, VisualizerDisplayMode.RING.next())
assertEquals(
    listOf("ART 1/4", "RADAR 2/4", "BANDS 3/4", "RING 4/4"),
    VisualizerDisplayMode.entries.map { it.label },
)
assertEquals(
    listOf("Album art", "Circular radar", "Spectrum bars", "Terminal spectrum ring"),
    VisualizerDisplayMode.entries.map { it.accessibilityName },
)
```

Run the focused test and confirm compilation fails because `RING` does not exist.

**Implementation:**

Replace `TUNNEL` with `RING` in the enum and explicit `next()` mapping. Do not change the mode count or ordering.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerDisplayModeTest'
```

The display-mode contract passes.

### Task 2: Define the calm tilted ellipse and adaptive spikes (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Replace the calm-tunnel test with a calm-ring test that requires:

- `RingGeometry` exposes center, horizontal/vertical radii, closed base-ring points, spike segments, orbit phase, and optional echo points.
- A 320-by-320 canvas produces 96 spikes centered at `(160f, 160f)` with radii `89.6f` and `48f` before bass response.
- Canvases with smaller dimensions of 120, 220, and 320 produce 64, 80, and 96 spikes.
- Each spike base lies on the projected ellipse within floating-point tolerance.
- Every spike's dot product with the ellipse gradient is positive, proving its tip follows the outward projected normal.
- Vertical compression makes the vertical radius smaller than the horizontal radius, every point is finite and within bounds, orbit phase is zero at `frameId = 0`, and echo points are absent.

Run the focused geometry test and confirm it fails because ring types and `ringGeometry` do not exist.

**Implementation:**

Add immutable `RingSpike` and `RingGeometry` data classes. Implement the safe center, fixed ellipse ratios, adaptive counts, base-ring sampling, normalized ellipse-gradient direction, calm baseline spike length, depth, and 4% safety inset. Return empty geometry for non-positive or non-finite dimensions. Keep tunnel types temporarily until the renderer is replaced.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerGeometryTest'
```

Radar, spectrum, and calm-ring geometry tests pass.

### Task 3: Add deterministic orbit and waveform distribution (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Add focused tests proving:

- Equal frame, dimensions, and magnitude state produce equal geometry.
- Advancing `frameId` changes orbit phase and moves a non-flat waveform pattern without changing the ellipse center or spike count.
- `frameId = 1_440` wraps phase to zero; negative IDs still produce phase in `0f..<2π`.
- An unchanged frame ID produces unchanged geometry, providing paused-frame stability.
- Empty waveform and a zero-filled waveform produce the same calm ring.
- A single high waveform region is resampled around the ring and advances to a different spike after sufficient orbit progress.

**Implementation:**

Add pure helpers to sanitize and linearly resample absolute waveform magnitudes at each spike angle. Apply the bounded 1,440-frame phase to sample lookup, then apply circular adjacent smoothing with `.25f / .50f / .25f` weights. Do not use wall-clock time, Compose animation, delays, coroutines, or mutate `AudioAnalysisFrame`.

**Verify:** Run the focused geometry suite. Orbit, wrap, pause, empty-waveform, and distribution cases pass.

### Task 4: Map bass, mid, and high energy into bounded geometry (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Create frames that vary one sanitized input at a time and assert:

- `lowEnergy = 1f` increases both ellipse radii by exactly 8% without moving the center.
- With a non-flat waveform, `midEnergy = 1f` increases primary spike lengths relative to the same waveform at zero mids.
- `highEnergy = 1f` changes fine tip lengths without changing base points or radii.
- Total spike length never exceeds 18.5% of the smaller dimension.
- Lower-half spikes have greater depth than their upper-half counterparts.
- NaN, infinity, and out-of-range samples/energies produce finite geometry inside the safety inset.
- Tiny positive and zero-size canvases do not produce inverted or invalid geometry.

**Implementation:**

Sanitize energy values to `0f..1f` and waveform samples to `-1f..1f`. Apply the fixed 8% bass radius pulse, 1.5% baseline, 13% waveform/mid response, and deterministic high-frequency contribution capped at 4%. Clamp combined spike length and every base/tip point to the fixed safety inset. Keep energy mappings independent enough for the isolated tests.

**Verify:** Run the focused geometry suite. All energy-isolation, depth, malformed-input, tiny-canvas, and bounds cases pass.

### Task 5: Replace tunnel history with ring smoothing and echo state (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/RingStateTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/RingState.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/TunnelHistoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TunnelHistory.kt`

**Test first:**

Create `RingStateTest` for a pure `updateRingState` reducer and assert:

- The first active ring frame becomes current without previous magnitudes.
- A strictly increasing active frame retains only the immediately previous magnitudes and blends `.35f` previous with `.65f` current.
- A non-increasing frame ID resets smoothing before accepting the new frame.
- ART, RADAR, BANDS, idle, and unavailable input return `RingState.Empty`.
- Effects disabled retains smoothing but never starts or retains echo state.
- A transient below `.65f` creates no echo; a qualifying transient creates one echo with age zero.
- Four subsequent increasing frames advance echo age through 1, 2, and 3 and then remove it.
- Mode exit, inactive state, and non-increasing frame ID clear echo state immediately.

Run the test and confirm it fails because `RingState` is absent.

**Implementation:**

Replace `TunnelHistory` with immutable `RingState` containing the current frame ID, current smoothed magnitudes, previous magnitudes needed for the next blend, and optional echo age/intensity. Add a pure `updateRingState(state, mode, frame, effectsEnabled)` reducer. Reuse the geometry resampling helper so reducer and projection agree on spike count. Retain no raw-frame history and at most one echo. Delete `TunnelHistory.kt` and `TunnelHistoryTest.kt` after replacement tests pass.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*RingStateTest'
```

All smoothing, echo lifetime, and reset tests pass, and no tunnel-history files remain.

### Task 6: Project the expanding transient echo (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Add tests that pass reducer-produced echo ages to `ringGeometry` and assert:

- No echo state produces no echo points.
- Ages 0, 1, 2, and 3 expand the ellipse by 3%, 6%, 9%, and 12%.
- Later echo ages have lower normalized opacity metadata than earlier ages.
- Echo point count matches the spike count and remains finite and inside the safety inset.
- Effects disabled produces no echo geometry even if stale echo state is supplied defensively.

**Implementation:**

Extend `RingGeometry` with optional `RingEcho(points, alpha)`. Project the ellipse at the fixed age-dependent scale, clamp it to the canvas inset, and use a four-step decreasing alpha. Do not modify the current ring radii or create multiple echoes.

**Verify:** Run the focused geometry suite. Expansion, fade, disabled-effects, and bounds tests pass.

### Task 7: Render the perspective spectrum ring (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizersTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`

**Test first:**

Replace tunnel Compose expectations with ring expectations:

- Active ring exposes `visualizer-ring` and not `visualizer-tunnel`.
- Unavailable ring remains mounted under `visualizer-ring` and displays `SIGNAL UNAVAILABLE`.
- Effects enabled exposes existing `scanlines`; effects disabled does not.
- The ring node has no click action of its own; cycling remains the deck's responsibility.

Run Android-test compilation and confirm it fails on the missing enum/tag.

**Implementation:**

Replace remembered tunnel history with `RingState`, updated from `LaunchedEffect(mode, frame.frameId, frame.status, effectsEnabled)`. In the `RING` branch:

1. Obtain current geometry using the reducer's smoothed magnitudes.
2. Draw the faint closed base ellipse.
3. Sort spikes by depth and draw far-to-near.
4. With effects enabled, draw a dim under-stroke 3 px wider than each spike.
5. Draw each sharp spike with the fixed depth tier color and `0.8f..2f` stroke width.
6. Draw the optional bright echo ellipse last using its geometry alpha.

Use only existing phosphor colors. Preserve radar, bands, unavailable fallback, border, scanlines, and sync-control layering.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
```

Run `TerminalVisualizersTest` on a connected device when available.

### Task 8: Update deck cycling and remove tunnel implementation (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/RingSourceGuardTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/TunnelSourceGuardTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Change `VisualizerDeckTest` to require `visualizer-ring` after the third advance and verify the next tap returns to art. In the sync-control test, require the controls to remain visible and functional while `visualizer-ring` stays selected.

Replace `TunnelSourceGuardTest` with `RingSourceGuardTest` that scans current app source and rejects:

```text
VisualizerDisplayMode.TUNNEL
tunnelGeometry
TunnelGeometry
TunnelHistory
visualizer-tunnel
Kaleidoscope tunnel
```

Run the unit guard and Android-test assembly; both must fail before cleanup.

**Implementation:**

Remove tunnel data classes, projection functions, constants, imports, renderer paths, history, and active tests. Rename the source guard and update deck expectations. Do not scan or edit historical approved specs and plans, which intentionally preserve prior decisions.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*RingSourceGuardTest' --tests '*VisualizerGeometryTest' --tests '*VisualizerDisplayModeTest'
.\gradlew.bat assembleDebugAndroidTest
```

No current source or active test references the obsolete tunnel implementation.

### Task 9: Replace tunnel previews and screenshot references (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/*.png`

**Test first:**

Rename `tunnelFrame` to `ringFrame` and replace the three tunnel previews with four `@PreviewTest` previews:

- `VisualizerRingPreview`: representative active frame, effects enabled, no transient.
- `VisualizerRingEffectsOffPreview`: the same frame with effects disabled.
- `VisualizerRingQuietPreview`: low-energy, low-amplitude waveform with effects enabled.
- `VisualizerRingTransientPreview`: the representative frame with `transient = 1f` and echo state represented deterministically by the preview frame/reducer path.

Run screenshot validation first and confirm it fails because the ring references do not exist.

**Implementation:**

Generate the four new references and inspect them before accepting:

- The ring reads as a tilted ellipse with spikes perpendicular to its rim.
- Near spikes are brighter/heavier and far spikes are dimmer/thinner.
- The active frame is visibly varied while the quiet frame remains calm and legible.
- Effects-on glow is restrained; effects-off remains crisp.
- The transient shows one bounded green echo without a full-screen flash.
- All images remain black and phosphor green with no clipping.

Delete only the three obsolete `VisualizerTunnel*` reference PNGs. Do not update unrelated references.

**Verify:** Run:

```powershell
.\gradlew.bat updateDebugScreenshotTest
.\gradlew.bat validateDebugScreenshotTest
```

Only the four reviewed ring references are added and the three tunnel references removed.

### Task 10: Update Pixel 7 verification wording (2–5 min)

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Replace active checklist references to tunnel with ring and add or revise unchecked checks for:

- The exact art → radar → bands → ring → art cycle.
- Visible slow orbit during playback and a frozen pattern while paused.
- Readable response during quiet, balanced, bass-heavy, treble-rich, and percussion-heavy passages.
- A brief non-flashing transient echo.
- Clean smoothing/echo reset after seek, track change, effects toggle, and mode re-entry.
- Effects-on/off readability, shared sync adjustment, and 30-minute comfort/performance.

**Implementation:**

Run these checks on the Pixel 7 when attached. Record clipping, stutter, stale echo, audio glitches, heat, battery, or sync inconsistencies as blockers. Do not add tuning controls or alter analysis behavior in response without a separate approved change.

**Verify:** Every ring checklist entry is checked or explicitly reported as pending because no device is available.

### Task 11: Run regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–10

**Test first:**

Run source and scope guards:

```powershell
rg -n 'VisualizerDisplayMode\.TUNNEL|tunnelGeometry|TunnelGeometry|TunnelHistory|visualizer-tunnel|Kaleidoscope tunnel' app/src
rg -n 'delay\(|rememberInfiniteTransition|infiniteRepeatable|System\.currentTimeMillis|elapsedRealtime' app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer
rg -n 'Color\(|Red|Blue|Yellow|Magenta|Cyan' app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt
```

The first and third commands return no matches. The second may report the existing temporary-label delay in `VisualizerDeck.kt`; ring geometry, state, and renderer must contain no independent timer.

**Implementation:**

Fix only failures introduced by this feature. Do not change `AudioAnalysisFrame`, PCM capture, analyzer behavior, playback, persistence, database code, dependency versions, palette, sync-offset limits, or the visualizer count.

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

Confirm the diff contains only the approved ring replacement, its design/plan, tests, references, and checklist updates plus any pre-existing user changes.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] Every new production behavior was introduced after a failing test or contract check.
- [ ] The deck cycles exactly through art, radar, bands, ring, and art.
- [ ] The fourth mode is labeled `RING 4/4`, announced as `Terminal spectrum ring`, and tagged `visualizer-ring`.
- [ ] Ring geometry is deterministic, tilted, finite, bounded, perspective-aware, and driven only by existing analysis frames.
- [ ] The pattern orbits slowly only on fresh active frames and freezes on an unchanged frame.
- [ ] Waveform, bass, mids, highs, transient, and frame ID have isolated tested effects.
- [ ] Smoothing retains one prior magnitude set; effects-enabled state retains no more than one four-frame echo.
- [ ] Near/far depth treatment and the existing phosphor palette remain readable with effects on and off.
- [ ] Obsolete tunnel renderer, geometry, history, current tests, tags, and screenshot references are removed.
- [ ] Existing visualizer sync controls apply unchanged in ring mode.
- [ ] No PCM, analyzer, playback, persistence, database, dependency, palette, or visualizer-count changes are introduced.
- [ ] Unit tests, Android-test assembly, screenshot validation, lint, and debug build pass.
- [ ] Connected tests and Pixel 7 visual, sync, comfort, and performance checks pass when a device is available, or are explicitly reported pending.
- [ ] `git diff --check` passes and no unrelated files are modified.
