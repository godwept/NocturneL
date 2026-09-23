# Full-Screen Visualizer Refinement Implementation Plan

**Date:** 2026-09-23  
**Design doc:** `docs/specs/2026-09-23-portrait-full-screen-visualizers-design.md`  
**Status:** Ready for review

## Overview

Refine the already-implemented portrait full-screen Radar and Grid based on physical Pixel testing. Radar's extended live sweep will begin at the outer radar circle and fade gradually from that boundary to the viewport edge instead of using one uniformly faint full-length line. The portrait Grid will keep its current square-cell portrait geometry and anchor distribution, but hotspot distance will become aspect-ratio aware so blobs remain approximately circular/symmetrical in physical screen space like the regular square Grid. NOW visualizers, Spectrum, audio analysis, pulse behavior, gestures, and navigation remain unchanged.

## Tasks

### Task 1: Add pure Radar extension fade geometry

**Files:**  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`

**Test first:**

Add focused tests for a new pure helper that describes the extended Radar beam from the outer circle to the viewport boundary, for example `radarExtendedBeamSegments(...)`.

Cover:

- the first segment starts at `radarSweepEndpoint(center, outerRadius, sweepDegrees)`;
- the final segment ends at `radarViewportEndpoint(center, width, height, sweepDegrees)`;
- segment positions advance monotonically away from the outer circle toward the edge;
- alpha starts highest at the outer boundary and decreases monotonically toward the viewport edge;
- the final alpha is positive but lower than the starting alpha;
- equivalent sweep angles such as 0°/360° produce identical segment geometry;
- invalid/zero viewport sizes return an empty segment list.

Use a small fixed segment count so the fade is smooth without introducing a shader or animation system.

**Implementation:**

Add a lightweight data class such as:

`RadarExtendedBeamSegment(start, end, alpha)`

and a pure helper that:

- takes radar center, outer radius, viewport width/height, sweep angle, start alpha, end alpha, and segment count;
- calculates the outer-circle start point and viewport-edge end point;
- divides only the **outside-core** distance into equal linear segments;
- interpolates alpha from the brighter boundary value to the faint edge value;
- never changes the existing `radarGeometry`, `radarSweepEndpoint`, or core Radar sweep.

Do not add gradients, RenderEffect, shaders, or new animation state.

**Verify:**

Run:

`.\gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ui.playback.visualizer.VisualizerGeometryTest"`

All existing and new geometry tests pass.

---

### Task 2: Render the Radar extension as a boundary-to-edge fade

**Files:**  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/FullScreenRadarEffects.kt`  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerRenderingStyleTest.kt`  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`

**Test first:**

Update/add style tests to require:

- the extended beam start alpha is greater than the current uniform `.14f` implementation;
- end alpha is lower than start alpha but remains greater than zero;
- both remain below the crisp core sweep alpha `.90f`;
- the wake remains weaker than the live extension at the boundary.

Add a source/ordering guard asserting the live extended beam is drawn from precomputed outside-core segments and is still rendered before the existing Radar bloom/core.

**Implementation:**

Replace the current single `center → viewport edge` extended line in `drawRadarExtendedBeam`.

New behavior:

- obtain the Radar outer radius from `geometry.gridRadii.last()`;
- create the fade only from that radius outward;
- draw the returned short line segments in order using their interpolated alpha;
- use the existing visualizer peak color and existing restrained beam width;
- leave the live core sweep and bloom untouched;
- leave the phosphor wake unchanged unless required only to preserve its lower visual hierarchy.

Centralize start/end alpha constants in `FullScreenRadarEffects.kt`. Use conservative values where the boundary alpha is visibly connected to the core bloom and the edge alpha is faint but still present.

**Verify:**

Run:

`.\gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ui.playback.visualizer.VisualizerRenderingStyleTest" --tests "ca.stewark.nocturnel.ui.playback.visualizer.VisualizerSourceGuardTest" --tests "ca.stewark.nocturnel.ui.playback.visualizer.VisualizerGeometryTest"`

All tests pass.

---

### Task 3: Make portrait Grid hotspot distance aspect-ratio aware

**Files:**  
`app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`

**Test first:**

Add a test such as `portraitGridHotspotsRemainPhysicallySymmetrical`.

Use one active band whose anchor is away from the screen edges on a 300×600 portrait field.

Determine the peak cell, then compare the live intensity at cells approximately the same **physical pixel distance** left/right and above/below the peak.

Assert:

- left/right intensities are approximately equal;
- above/below intensities are approximately equal;
- horizontal and vertical samples at equal physical distance have similar intensity within a reasonable tolerance;
- the same test against the square Grid remains unchanged.

Add a second regression assertion that the portrait Grid still spans the upper and lower portions of the screen and still uses more than 30 rows, proving the fix did not revert it to a square.

The new test should fail with the current normalized-distance calculation because normalized Y maps to roughly twice the physical distance on a tall field.

**Implementation:**

Refactor `blendedHotspotIntensity` so the caller can provide a Y-distance scale.

For the regular square Grid:

- use a scale of `1f`, preserving current math exactly.

For the portrait Grid:

- calculate `contentHeight / contentWidth`;
- multiply normalized Y delta by that aspect ratio before `hypot`;
- leave normalized X delta unchanged.

Conceptually:

`distance = hypot(dx, dy * physicalAspectRatio)`

This makes a hotspot radius defined relative to width appear circular in physical pixels instead of vertically stretched.

Keep:

- square cell geometry;
- current portrait row count;
- current portrait anchor list;
- hotspot radius constant;
- blending formula;
- ghost/live intensity behavior.

Do not rearrange anchors in this task.

**Verify:**

Run:

`.\gradlew.bat :app:testDebugUnitTest --tests "ca.stewark.nocturnel.ui.playback.visualizer.VisualizerGeometryTest"`

All regular-Grid and portrait-Grid tests pass.

---

### Task 4: Verify no regression to NOW Grid or Radar

**Files:**  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`  
`app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`

**Test first:**

Retain and, where useful, tighten existing assertions that:

- `frequencyGridGeometry` still returns exactly 900 cells;
- the regular square Grid remains centered;
- regular Grid anchor peaks remain unique/distributed;
- standard Radar still uses `radarSweepEndpoint` to stop the live core sweep at the outer circle;
- the fading extension remains guarded by `expanded && effectsEnabled`;
- NOW does not use `radarExtendedBeamSegments` or portrait hotspot aspect correction.

**Implementation:**

No production change should be necessary. If these tests fail, correct only the expanded rendering branch or shared helper defaults so the standard visualizers retain their original behavior.

**Verify:**

Run the focused geometry and source-guard test classes.

---

### Task 5: Refresh only the changed full-screen screenshot references

**Files:**  
`app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/FullScreenRadarPreview_Full screen radar_a6b975a7_0.png`  
`app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/FullScreenGridPreview_Full screen grid_4959e21e_0.png`

**Test first:**

Run:

`.\gradlew.bat :app:validateDebugScreenshotTest`

Expect only the full-screen Radar and/or Grid references to differ. No unrelated screenshot should fail.

**Implementation:**

Regenerate screenshot references only after the unit tests are green.

Review the generated Radar and Grid images before accepting them:

- Radar extension visibly continues beyond the outer circle and fades gradually toward the edge.
- Grid hotspots look rounder/more symmetrical and no longer vertically stretched.
- Spectrum and unrelated UI references remain unchanged.

Update only the affected Radar/Grid reference files.

**Verify:**

Run:

`.\gradlew.bat :app:validateDebugScreenshotTest`

All screenshot tests pass.

---

### Task 6: Full regression, Pixel verification, and CI

**Files:** No additional source files expected.

**Test first / verification:**

Run the complete local regression:

`.\gradlew.bat :app:testDebugUnitTest`

`.\gradlew.bat :app:validateDebugScreenshotTest`

`.\gradlew.bat :app:assembleDebugAndroidTest`

`.\gradlew.bat :app:lintRelease`

`.\gradlew.bat :app:bundleRelease`

`.\gradlew.bat :app:assembleDebug`

Then:

`git diff --check`

`git status --short`

**Physical Pixel check:**

Radar:
- core sweep looks unchanged;
- beam visibly continues across the outer circle instead of appearing to stop there;
- fade begins at the outer circle, not at the center;
- fade remains gradual for most of the distance to the screen edge;
- edge remains faint;
- wake stays secondary.

Grid:
- cells still fill the portrait screen;
- blobs appear approximately round/symmetrical rather than vertically stretched;
- activity still uses upper, middle, and lower regions;
- overall behavior still resembles the regular Grid visualizer.

Regression:
- NOW Radar and Grid unchanged;
- Spectrum unchanged;
- beat pulse unchanged;
- swipe/Exit/Back/immersive behavior unchanged.

After physical verification, push the completed batch and monitor the resulting Android CI run until green. If CI fails, inspect and fix only failures caused by this refinement, then continue monitoring.

## Definition of Done

- [ ] Radar extended live beam begins at the outer circular boundary.
- [ ] Extended beam fades monotonically and gradually from that boundary to the viewport edge.
- [ ] Radar core sweep, pulse, and NOW presentation remain unchanged.
- [ ] Portrait Grid hotspot falloff is aspect-ratio corrected.
- [ ] Portrait Grid blobs are approximately symmetrical in physical screen space.
- [ ] Portrait Grid remains a full-height square-cell field with its current deterministic anchors.
- [ ] Regular square Grid remains unchanged.
- [ ] Spectrum and unrelated UI remain unchanged.
- [ ] Unit tests pass.
- [ ] Screenshot validation passes with only intentional Radar/Grid reference updates.
- [ ] Instrumented tests compile, lint passes, release bundle builds, and debug APK builds.
- [ ] Physical Pixel verification passes.
- [ ] GitHub Actions is green.
