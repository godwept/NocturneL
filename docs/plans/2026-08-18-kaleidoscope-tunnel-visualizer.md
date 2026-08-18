# Kaleidoscope Tunnel Visualizer Implementation Plan

**Date:** 2026-08-18  
**Design doc:** `docs/specs/2026-08-18-kaleidoscope-tunnel-visualizer-design.md`  
**Status:** Ready for review

## Overview

Replace the fourth visualizer mode's oscilloscope with a restrained, four-way symmetric kaleidoscope tunnel. The implementation will rename the mode, project existing immutable PCM analysis frames into deterministic nested polygon geometry, render current and short-lived historical layers with the existing phosphor palette, replace scope screenshots and device checks, and remove all now-unused scope code. PCM capture, analysis, playback, and the newly added visualizer sync-offset path remain unchanged.

## Fixed Implementation Decisions

- Rename the enum entry to `TUNNEL`, its label to `TUNNEL 4/4`, its accessibility name to `Kaleidoscope tunnel`, and its scene tag to `visualizer-tunnel`.
- Generate three layers below a 96 px minimum dimension, five below 192 px, and seven otherwise.
- Represent each layer as a closed list of 32 points: eight equal segments on each side of a four-sided polygon inscribed in a circle around the fixed canvas center.
- Keep every layer inside a maximum corner radius of 46% of the canvas's smaller dimension, leaving a 4% safety inset under any rotation.
- Derive depth phase as `floorMod(frameId, 120) / 120f`; at the analyzer's approximately 30 fps rate, one layer interval takes about four seconds.
- Derive rotation as `floorMod(frameId, 1800) * 0.2f` degrees; this produces one restrained revolution per approximately 60 seconds and never uses an independent timer.
- Space layer corner radii between 8% and 46% of the smaller dimension. Apply a `1f + lowEnergy * 0.20f` spacing exponent plus at most a 4% bass pulse, then sort layers from inner to outer.
- Fold the 128-point waveform into one mirrored nine-position edge profile and repeat the same profile on all four sides. Cap waveform displacement at 3.5% of each layer radius.
- Pull corners inward by at most 4% of layer radius from mid energy. Add a four-cycle edge ripple capped at 2.5% of layer radius from high energy.
- Emit no echo when `transient == 0f`. Otherwise create exactly one copy of the outer layer expanded by at most 8%, clipped to the 46% maximum radius.
- Retain at most three prior active `AudioAnalysisFrame` values only while tunnel mode and CRT effects are both active. Draw them oldest-to-newest at muted alpha `.08f`, `.12f`, and `.16f` before the current frame.
- Draw current layers inner-to-outer with alpha increasing from `.35f` to `.90f` and stroke widths from `1f` to `1.75f`. With effects enabled, add one restrained 4 px dim glow behind current paths; effects disabled draws only crisp paths.

## Tasks

### Task 1: Rename the fourth display mode (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDisplayModeTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDisplayMode.kt`

**Test first:**

Change `VisualizerDisplayModeTest` to require:

```kotlin
assertEquals(VisualizerDisplayMode.TUNNEL, VisualizerDisplayMode.BANDS.next())
assertEquals(VisualizerDisplayMode.ART, VisualizerDisplayMode.TUNNEL.next())
assertEquals(
    listOf("ART 1/4", "RADAR 2/4", "BANDS 3/4", "TUNNEL 4/4"),
    VisualizerDisplayMode.entries.map { it.label },
)
assertEquals(
    listOf("Album art", "Circular radar", "Spectrum bars", "Kaleidoscope tunnel"),
    VisualizerDisplayMode.entries.map { it.accessibilityName },
)
```

Run the focused test and confirm it fails because `TUNNEL` does not exist.

**Implementation:**

Replace `SCOPE` with `TUNNEL` in the enum and explicit `next()` mapping. Do not change the number or ordering of modes.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerDisplayModeTest'
```

The mode contract passes.

### Task 2: Define calm nested tunnel geometry (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Replace the scope mapping test with calm-tunnel assertions against an active zero-energy frame at `frameId = 0`:

- A 320-by-320 canvas produces seven `TunnelLayer` values.
- Each layer contains exactly 32 points and a normalized depth in `0f..1f`.
- Layer corner radii increase inner-to-outer and do not exceed `147.2f` (`320 * .46`).
- Rotating every point 90 degrees around `TunnelGeometry.center` maps it onto another point in the same layer within a small floating-point tolerance.
- All points are inside `0f..320f`.
- The center is exactly `(160f, 160f)`, depth phase and rotation are zero, and `echoLayer` is null.
- Canvas sizes 80, 160, and 320 produce three, five, and seven layers respectively.

Run the geometry test and confirm it fails because the tunnel types and projection do not exist.

**Implementation:**

Add:

```kotlin
internal data class TunnelLayer(val points: List<VisualizerPoint>, val depth: Float)
internal data class TunnelGeometry(
    val center: VisualizerPoint,
    val layers: List<TunnelLayer>,
    val echoLayer: TunnelLayer?,
    val rotationDegrees: Float,
    val depthPhase: Float,
)
internal fun tunnelGeometry(frame: AudioAnalysisFrame, width: Float, height: Float): TunnelGeometry
```

Implement the fixed center, adaptive layer counts, bounded depth/rotation phases, 8%–46% calm radii, and four equal sides with eight segments each. For this slice, zero input produces undeformed frames. Use floor-mod behavior for negative or wrapped `frameId` values so phase remains bounded.

Keep the old scope functions temporarily until the new renderer replaces them.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerGeometryTest'
```

Radar, spectrum, and calm-tunnel geometry tests pass.

### Task 3: Add deterministic PCM-frame motion (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Add tests proving:

- Calling `tunnelGeometry` twice with the same frame and dimensions returns equal geometry.
- Advancing `frameId` from 0 to 1 changes both depth phase and rotation.
- `frameId = 120` wraps depth phase to zero.
- `frameId = 1_800` wraps rotation to zero.
- A negative frame ID still produces depth phase in `0f..<1f` and rotation in `0f..<360f`.
- Advancing `frameId` changes geometry without changing layer count, point count, center, or bounds.

**Implementation:**

Apply the fixed phase values to layer positions. Wrap a layer crossing the outer boundary back toward the center, then sort generated layers by radius/depth so drawing order is stable. Rotate all layer points around the fixed center by the bounded rotation angle. Do not introduce Compose animation, coroutine delay, wall-clock time, or another state source.

**Verify:** Run the focused geometry test. All deterministic motion and existing geometry cases pass.

### Task 4: Map waveform and frequency energy into symmetric deformation (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Create separate frames that vary only one input at a time and assert:

- `lowEnergy = 1f` changes layer spacing and outer pulse without changing center or point counts.
- `midEnergy = 1f` pulls corner points inward more than side-midpoint points.
- `highEnergy = 1f` changes intermediate edge points while leaving the four-way rotational symmetry intact.
- A deterministic non-flat waveform changes edge points from the calm geometry.
- The folded profile is identical on all four sides and mirrored around each side's midpoint.
- Empty waveform and bands collections produce the same undeformed layer structure as zero-filled inputs.
- NaN, infinite, and out-of-range inputs are sanitized to finite bounded points.

**Implementation:**

Sanitize every analysis scalar and waveform sample to finite `0f..1f` or `-1f..1f` as appropriate. Build one nine-position waveform profile by sampling evenly across the available waveform, folding the second half onto the first, and mirroring positions around the edge midpoint. Repeat that profile on all four sides.

Apply the fixed bass spacing exponent/pulse, mid corner pull, waveform displacement, and high-frequency ripple before rotation. Clamp every final point to the square's 4% safety inset. Do not mutate `AudioAnalysisFrame` or add analysis fields.

**Verify:** Run the focused geometry suite. All energy-isolation, symmetry, malformed-input, and prior tests pass.

### Task 5: Add the single transient echo and small-canvas fallback (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Assert:

- Zero transient produces `echoLayer == null`.
- Positive transient produces exactly one echo layer with 32 finite points.
- Increasing transient moves the echo outward but never beyond the 46% radius or canvas bounds.
- A zero-by-zero canvas returns a centered geometry with no invalid points or exception.
- Tiny positive dimensions use three layers with finite, non-inverted geometry.

**Implementation:**

Create the echo by scaling the outer current layer according to the fixed 8% maximum, then clip it to the same safe radius and canvas bounds. For non-positive dimensions, return an empty-layer geometry at the non-negative center with null echo. Keep layer detail reduced according to the fixed size thresholds.

**Verify:** Run the focused geometry suite. Echo, zero-size, tiny-size, and all earlier tests pass.

### Task 6: Add a testable three-frame tunnel history reducer (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/TunnelHistoryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TunnelHistory.kt`

**Test first:**

Create pure reducer tests that assert:

- The first active tunnel frame becomes current with no prior frame.
- Each increasing frame ID moves the former current frame into `priorFrames`.
- Only the three newest prior frames are retained.
- A non-increasing frame ID clears prior history before accepting the new current frame.
- Art, radar, bands, idle, unavailable, and `effectsEnabled = false` all return empty history.
- Re-entering active tunnel mode after a clear begins with no prior frame.

**Implementation:**

Create immutable internal `TunnelHistory` state containing `priorFrames: List<AudioAnalysisFrame>` and `currentFrame: AudioAnalysisFrame?`, plus a pure:

```kotlin
internal fun updateTunnelHistory(
    history: TunnelHistory,
    mode: VisualizerDisplayMode,
    frame: AudioAnalysisFrame,
    effectsEnabled: Boolean,
): TunnelHistory
```

Retain only active frames with strictly increasing IDs while tunnel mode and effects are active. Do not store raw PCM, geometry, or more than three prior frames.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*TunnelHistoryTest'
```

All lifecycle and retention cases pass.

### Task 7: Render current and historical tunnel layers (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizersTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`

**Test first:**

Extend `TerminalVisualizersTest` to compose active and unavailable tunnel frames and assert:

- Active tunnel exposes `visualizer-tunnel` and not `visualizer-scope`.
- Effects enabled exposes `scanlines`; effects disabled does not.
- Unavailable tunnel displays `SIGNAL UNAVAILABLE` and remains mounted under `visualizer-tunnel`.
- Tunnel nodes have no click action of their own; deck interaction remains the parent responsibility.

Run Android-test compilation first and confirm missing `TUNNEL`/tag expectations fail.

**Implementation:**

Replace scope history with `remember { mutableStateOf(TunnelHistory()) }` updated from `LaunchedEffect(mode, frame.frameId, frame.status, effectsEnabled)`. In the `TUNNEL` renderer branch:

1. Reconstruct prior geometries at current canvas size and draw oldest-to-newest with the fixed muted alpha values.
2. Draw the current geometry's layers inner-to-outer using `Path.close()`, fixed depth-based alpha/stroke, and only existing phosphor colors.
3. When effects are enabled, draw a restrained dim glow behind current paths.
4. Draw the optional echo last in `PhosphorBright` with alpha derived from sanitized transient intensity.

Rename the tag to `visualizer-tunnel`. Preserve radar, bands, unavailable fallback, border, scanlines, and sync-control layering. Remove scope history and trace drawing from this file.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
```

Run the focused connected Compose test when a device is attached.

### Task 8: Update deck cycling and remove obsolete scope geometry (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:**

Change `VisualizerDeckTest` to require `visualizer-tunnel` after the third deck advance. In the sync-control test, confirm controls remain visible in tunnel mode, their clicks leave `visualizer-tunnel` selected, and the next deck tap returns to art.

Add a source assertion to `VisualizerGeometryTest` or a small source-guard test that rejects `scopeGeometry`, `visualizer-scope`, and `VisualizerDisplayMode.SCOPE` from current app source.

**Implementation:**

Remove both `scopeGeometry` overloads and any imports used only by the waveform-path renderer. Update deck-test expectations only; `VisualizerDeck` itself should need no behavioral change because it already follows `VisualizerDisplayMode.next()` and treats every non-art mode uniformly.

Do not edit historical approved design documents or plans containing the old term.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerGeometryTest' --tests '*VisualizerDisplayModeTest'
.\gradlew.bat assembleDebugAndroidTest
```

All unit tests pass and Android tests compile without scope APIs.

### Task 9: Replace scope previews with reviewed tunnel screenshots (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/*.png`

**Test first:**

Rename `scopeFrame` to `tunnelFrame`, retain its deterministic waveform, and give it distinct low/mid/high values. Replace scope previews with three `@PreviewTest` previews:

- `VisualizerTunnelPreview`: active restrained frame, effects enabled, transient low or zero.
- `VisualizerTunnelEffectsOffPreview`: the same frame, effects disabled.
- `VisualizerTunnelTransientPreview`: the same frame with `transient = 1f` and a distinct frame ID.

Run screenshot validation first. It must fail because the three tunnel references do not yet exist.

**Implementation:**

Generate the three references and inspect them before accepting:

- All show clear nested four-way depth and remain predominantly black/green.
- Effects-on adds restrained glow/persistence styling without blurring structure.
- Effects-off is crisp and still recognizably psychedelic.
- The transient adds one bright echo frame without a full-screen flash.
- Sync controls are unaffected in their separate existing reference.

Delete obsolete scope references if any are generated or tracked; current repository inspection shows none are tracked before this task. Do not update unrelated screenshots.

**Verify:** Run:

```powershell
.\gradlew.bat updateDebugScreenshotTest
.\gradlew.bat validateDebugScreenshotTest
```

Only the three reviewed tunnel references are added or intentionally changed.

### Task 10: Update active device verification wording (2–5 min)

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Replace current checklist mentions of `scope` in the mode cycle, transient alignment, and shared sync checks with `tunnel`. Add unchecked entries for:

- Restrained tunnel motion during quiet, balanced, bass-heavy, treble-rich, and percussion-heavy passages.
- A visible but non-flashing transient echo.
- Calm silence and pause behavior.
- Clean history reset after seek, track change, effects toggle, and leaving/re-entering tunnel mode.
- Effects-on/off readability and motion comfort over a 30-minute session.

**Implementation:**

Run the checks on the Pixel 7. Record any excessive motion, clipping, stutter, audio glitch, heat, or sync inconsistency as a blocker. Do not add user tuning controls or alter analyzer behavior during this task.

**Verify:** Every new checklist entry is checked or explicitly reported as a release blocker.

### Task 11: Run regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–10

**Test first:**

Run source guards:

```powershell
rg -n 'VisualizerDisplayMode\.SCOPE|scopeGeometry|visualizer-scope|Oscilloscope' app/src
rg -n 'delay\(|rememberInfiniteTransition|infiniteRepeatable|System\.currentTimeMillis|elapsedRealtime' app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer
rg -n 'Color\(|Red|Blue|Yellow|Magenta|Cyan' app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt
```

All commands return no tunnel-related violations. Existing temporary-label delay in `VisualizerDeck.kt` is allowed; the tunnel renderer and geometry must contain no timer-driven motion or new color definitions.

**Implementation:**

Fix only failures introduced by this feature. Do not change `AudioAnalysisFrame`, analyzer code, PCM capture/alignment, playback, persistence, sync-offset behavior, visualizer count, color settings, or unrelated UI.

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

Confirm the diff contains only the approved tunnel replacement, its tests/references/checklist, the approved tunnel design and plan, and the already-present visualizer sync-offset work.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] Every new production behavior was introduced after a failing test or contract check.
- [ ] The deck cycles exactly through art, radar, bands, tunnel, and art.
- [ ] The fourth mode is labeled `TUNNEL 4/4`, announced as `Kaleidoscope tunnel`, and tagged `visualizer-tunnel`.
- [ ] Tunnel geometry is deterministic, four-way symmetric, finite, bounded, and driven only by existing analysis frames.
- [ ] Bass, mids, highs, waveform, transient, and frame ID have isolated tested visual effects.
- [ ] Motion remains restrained, silence remains calm, and unavailable analysis retains its existing fallback.
- [ ] Effects enabled uses no more than three prior frames and existing phosphor styling; effects disabled is crisp and trail-free.
- [ ] Obsolete scope renderer, geometry, current tests, tags, and screenshot previews are removed.
- [ ] Visualizer sync controls behave unchanged in tunnel mode.
- [ ] No PCM, analyzer, playback, persistence, database, dependency, or palette changes are introduced.
- [ ] Unit tests, Android-test assembly, connected tests when available, screenshot validation, lint, and debug build pass.
- [ ] Pixel 7 comfort, sync, playback-safety, and extended-session checks pass.
- [ ] No unplanned files are modified.
