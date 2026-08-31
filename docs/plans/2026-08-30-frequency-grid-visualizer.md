# Frequency Grid Visualizer Implementation Plan

**Date:** 2026-08-30
**Design doc:** docs/specs/2026-08-30-frequency-grid-visualizer-design.md
**Status:** Ready for review

## Overview

Add a fourth `GRID` visualizer that maps the existing 32 spectrum bands to fixed scattered anchors on a responsive 30×30 cell field. Radial contributions blend into bright hotspots, reuse the current band afterglow when CRT Effects is enabled, and retain the existing visualizer controls and unavailable-signal behavior.

## Tasks

### Task 1: Add the fourth display mode

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDisplayModeTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDisplayMode.kt`

**Test first:** Update the cycle assertions to `ART → RADAR → BANDS → GRID → ART`; expect labels `ART 1/4`, `RADAR 2/4`, `BANDS 3/4`, `GRID 4/4` and accessibility name `Frequency grid`.

**Implementation:** Add `GRID("GRID 4/4", "Frequency grid")`, change the other denominators to `/4`, and extend `next()` in the tested order.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ui.playback.visualizer.VisualizerDisplayModeTest"`.

### Task 2: Define deterministic grid layout

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:** Assert that the grid contains 900 row-major cells; all cells are square, evenly gapped, centered, finite, and bounded on square, non-square, tiny, zero, and negative canvases. Assert exactly 32 unique normalized anchors, each inset from the edge, distributed across all four quadrants, and stable between calls.

**Implementation:** Add `FrequencyGridCell(left, top, size, liveIntensity, ghostIntensity)`, a private hand-tuned list of 32 normalized `VisualizerPoint` anchors, and `frequencyGridGeometry(...)`. Use a 30×30 grid, an 8 px maximum outer inset, and a gap equal to 24% of cell pitch; center the largest square that fits the canvas and return no cells when usable size is zero.

**Verify:** Run the `VisualizerGeometryTest` class; confirm geometry tests pass before adding intensity behavior.

### Task 3: Blend radial hotspot intensity

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`

**Test first:** For a single active band, assert its anchor cell is brighter than cells beyond its radius and intensity decreases with distance. Assert two overlapping active bands produce a brighter shared area without exceeding `1f`; malformed values become zero or clamp to `[0,1]`; empty/mismatched retained data produces no ghost intensity.

**Implementation:** For each cell center, evaluate all available band anchors with radius `0.18` of the grid side and smooth quadratic falloff `(1 - distance/radius)²`. Blend contributions with saturating union `1 - Π(1 - contribution)` rather than raw addition. Calculate live intensity from sanitized live levels and ghost intensity from the positive retained-minus-live contribution multiplied by each `BandAfterglow.alpha / BAND_AFTERGLOW_MAX_ALPHA`.

**Verify:** Run the `VisualizerGeometryTest` class; all old and new geometry tests pass.

### Task 4: Reuse band afterglow for GRID

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAfterglowTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerAfterglow.kt`

**Test first:** Assert GRID initializes and updates the band envelope, keeps radar state empty, clears on a mode/size change or frame rewind, and returns empty state when effects are disabled, motion is reduced upstream, or audio is idle/unavailable.

**Implementation:** Route `VisualizerDisplayMode.GRID` through the same `updateBandAfterglow` state branch as BANDS. Keep the existing lifecycle reset rules and 500 ms decay unchanged.

**Verify:** Run the `VisualizerAfterglowTest` class.

### Task 5: Render the phosphor cell field

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerRenderingStyleTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`

**Test first:** Add source/style assertions for the `visualizer-grid` tag, GRID branch, geometry call, ghost-before-live draw order, no platform blur, and approved opacity hierarchy: dark base cells < ghost cells < live cells < peak centers.

**Implementation:** Add the GRID test tag and drawing branch. Draw all 900 cells once in `visualizerSecondary` at low base alpha, overlay ghost intensity only when afterglow data exists, then overlay live intensity using `visualizerPrimary`; add `visualizerPeak` only for intensity above `0.80`. Keep hard square edges and gaps, draw no blur, and leave the shared border, scanlines, sync overlay, and `SIGNAL UNAVAILABLE` path untouched.

**Verify:** Run `VisualizerRenderingStyleTest` and `VisualizerSourceGuardTest` together.

### Task 6: Update user-facing visualizer documentation

**Files:** `app/src/test/java/ca/stewark/nocturnel/ProductSiteContractTest.kt`, `docs/_manual/visualizers.md`

**Test first:** Require `Frequency grid` in the visualizer manual contract.

**Implementation:** Change “three modes” to “four modes,” add **Frequency grid** as item four, and state that Radar, Spectrum, and Frequency Grid modes share sync controls. Do not change Play Store listing or graphics.

**Verify:** Run `.\gradlew.bat testDebugUnitTest --tests "ca.stewark.nocturnel.ProductSiteContractTest"`.

### Task 7: Full verification and debug APK

**Files:** No source changes expected.

**Test first:** Review `git diff --check` and `git status --short`; confirm only planned files changed and preserve the user's pre-existing deletions.

**Implementation:** Run `.\gradlew.bat testDebugUnitTest`, then `.\gradlew.bat assembleDebug`. Do not run release or Play Store tasks.

**Verify:** All unit tests pass and `app/build/outputs/apk/debug/app-debug.apk` exists.

## Definition of Done

- [ ] All tasks completed in order with tests written before implementation.
- [ ] The four-mode cycle and accessibility labels are correct.
- [ ] The fixed 30×30 grid blends all 32 bands and respects CRT afterglow.
- [ ] Existing visualizers and unavailable-signal behavior remain unchanged.
- [ ] Full unit tests pass and the debug APK builds.
- [ ] No unplanned files are modified; no Play Store release work is performed.
