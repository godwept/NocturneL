# Remove Ring Visualizer Implementation Plan

**Date:** 2026-08-19  
**Design doc:** `docs/specs/2026-08-19-remove-ring-visualizer-design.md`  
**Status:** Ready for review

## Overview

Remove the Terminal Spectrum Ring and reduce the display cycle to album art, radar, and spectrum bars. The implementation will establish the new three-mode contract in tests, remove the ring enum/renderer/state and geometry in compilable slices, delete ring previews and references, update the Pixel 7 checklist, and run the complete local quality gate without changing retained visualizers, playback, CRT effects, or shared sync behavior.

## Tasks

### Task 1: Establish the failing three-mode contract (2–5 min)

**Files:** `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDisplayModeTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/RingSourceGuardTest.kt`

**Test first:**

Change `VisualizerDisplayModeTest` to require exactly:

```kotlin
assertEquals(VisualizerDisplayMode.RADAR, VisualizerDisplayMode.ART.next())
assertEquals(VisualizerDisplayMode.BANDS, VisualizerDisplayMode.RADAR.next())
assertEquals(VisualizerDisplayMode.ART, VisualizerDisplayMode.BANDS.next())
assertEquals(listOf("ART 1/3", "RADAR 2/3", "BANDS 3/3"), VisualizerDisplayMode.entries.map { it.label })
assertEquals(listOf("Album art", "Circular radar", "Spectrum bars"), VisualizerDisplayMode.entries.map { it.accessibilityName })
```

Replace `RingSourceGuardTest` with `VisualizerSourceGuardTest`. Have it read only `src/main/java/ca/stewark/nocturnel/ui/playback/visualizer` and reject `VisualizerDisplayMode.RING`, `RingState`, `RingEchoState`, `RingGeometry`, `RingSpike`, `RingEcho`, `ringGeometry`, `ringMagnitudes`, `visualizer-ring`, and `Terminal spectrum ring`.

Run the focused unit tests and confirm they fail against the current four-mode source.

**Implementation:**

None in this task. This is the required red phase for the removal contract.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerDisplayModeTest' --tests '*VisualizerSourceGuardTest'
```

The command fails for the expected `/4`, RING, and ring-source assertions rather than an unrelated setup error.

### Task 2: Remove the ring mode, renderer, and state atomically (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDisplayMode.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizers.kt`, `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/RingState.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/RingStateTest.kt`

**Test first:**

Use the failing tests from Task 1. Also run Android-test compilation before implementation and record that existing RING branches prevent the new three-entry enum contract from compiling once the enum is removed.

**Implementation:**

- Change labels to `ART 1/3`, `RADAR 2/3`, and `BANDS 3/3`.
- Delete the `RING` enum entry and map `BANDS -> ART`.
- Remove remembered `RingState`, its `LaunchedEffect`, the `visualizer-ring` tag mapping, and the complete RING Canvas branch from `TerminalVisualizers.kt`.
- Remove imports used only by ring path drawing while preserving radar and bands rendering byte-for-byte where practical.
- Delete `RingState.kt` and `RingStateTest.kt`.

Do not alter the unavailable fallback, border, scanlines, radar, bands, or sync UI.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerDisplayModeTest'
.\gradlew.bat assembleDebugAndroidTest
```

The three-mode enum passes and production plus Android tests compile far enough to expose only remaining test expectations that still mention RING.

### Task 3: Remove ring geometry and retain radar/bands coverage (2–5 min)

**Files:** `app/src/main/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometry.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerGeometryTest.kt`, `app/src/test/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerSourceGuardTest.kt`

**Test first:**

Confirm `VisualizerSourceGuardTest` still fails because ring geometry remains. Preserve the existing radar test that checks 32 band spokes, sweep motion, transient echo, and bounds, plus the spectrum test that checks 32 ordered bounded columns.

**Implementation:**

Delete `RingSpike`, `RingEcho`, `RingGeometry`, `ringGeometry`, `ringMagnitudes`, circular sampling/resampling helpers, ring-only sanitizers, bounds helpers, and all `RING_*` constants. Remove every ring-specific test from `VisualizerGeometryTest`, leaving its RADAR and BANDS regression cases unchanged. Remove imports used only by ring math.

**Verify:** Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*VisualizerGeometryTest' --tests '*VisualizerSourceGuardTest'
```

Both retained geometry tests pass and the source guard finds no ring implementation in current production source.

### Task 4: Update Compose and deck tests for three modes (2–5 min)

**Files:** `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/TerminalVisualizersTest.kt`, `app/src/androidTest/java/ca/stewark/nocturnel/ui/playback/visualizer/VisualizerDeckTest.kt`

**Test first:**

Replace RING-based UI setup and expectations before touching any additional production code:

- Use BANDS for the effects-disabled unavailable scene, assert `SIGNAL UNAVAILABLE`, `visualizer-bands`, and no scanlines.
- Remove the active-ring test; retain the active RADAR/scanlines test.
- In the deck cycle test, require ART -> RADAR -> BANDS -> ART with no fourth tap.
- In the sync test, verify controls remain visible on RADAR and BANDS, sync clicks leave the selected mode unchanged, and the next deck tap from BANDS returns to ART and hides controls.

**Implementation:**

No further production change should be necessary. If compilation identifies an active RING reference, remove only that stale reference rather than changing retained behavior.

**Verify:** Run:

```powershell
.\gradlew.bat assembleDebugAndroidTest
```

All Compose tests compile against the three-mode contract. Run connected tests when a device is available.

### Task 5: Remove ring previews and reference images (2–5 min)

**Files:** `app/src/screenshotTest/java/ca/stewark/nocturnel/ui/TerminalUiScreenshotTest.kt`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/VisualizerRingPreview_Visualizer ring_c838be68_0.png`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/VisualizerRingEffectsOffPreview_Visualizer ring effects off_03b45d2c_0.png`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/VisualizerRingQuietPreview_Visualizer ring quiet_2d7775bc_0.png`, `app/src/screenshotTestDebug/reference/ca/stewark/nocturnel/ui/TerminalUiScreenshotTestKt/VisualizerRingTransientPreview_Visualizer ring transient_ed81a1d4_0.png`

**Test first:**

Remove `ringFrame` and the four `VisualizerRing*Preview` functions, then run screenshot validation. It must identify the four now-obsolete ring references as unused or otherwise complete without requiring changes to retained references, depending on the screenshot plugin's stale-reference behavior.

**Implementation:**

Delete exactly the four listed ring PNGs after resolving and confirming their paths remain inside `app/src/screenshotTestDebug/reference`. Do not regenerate or modify RADAR, BANDS, sync-controls, or unrelated screenshot references.

**Verify:** Run:

```powershell
.\gradlew.bat validateDebugScreenshotTest
```

Screenshot validation passes, no `VisualizerRing*` preview/reference remains, and retained reference hashes are unchanged.

### Task 6: Update the Pixel 7 checklist (2–5 min)

**Files:** `docs/testing/pixel-7-release-checklist.md`

**Test first:**

Search active checklist text for `ring`, `three visualizers`, and the four-mode cycle. Replace those checks with:

- Exact album art -> radar -> bands -> album art cycling.
- Kick/snare alignment in radar and bands.
- CRT readability for both retained visualizers.
- Shared sync adjustment and Bluetooth calibration across radar and bands.
- A 30-minute retained-visualizer playback check.

Remove ring-specific orbit, echo, smoothing, pause, re-entry, and comfort checks.

**Implementation:**

Keep the revised entries unchecked until run on a Pixel 7. Do not change unrelated playback, queue, library, or device checks.

**Verify:** Run:

```powershell
rg -n 'ring|tunnel|three visualizers|1/4|2/4|3/4|4/4' docs/testing/pixel-7-release-checklist.md
```

The command returns no obsolete visualizer-cycle wording. Device checks are explicitly pending when no device is attached.

### Task 7: Run final regression and scope checks (2–5 min)

**Files:** all files changed by Tasks 1–6

**Test first:**

Run source and asset guards:

```powershell
rg -n 'VisualizerDisplayMode\.RING|RingState|RingEchoState|RingGeometry|RingSpike|RingEcho|ringGeometry|ringMagnitudes|visualizer-ring|Terminal spectrum ring' app/src/main
rg --files app/src | rg 'Ring|ring'
```

The first command returns no production matches. The second may return only the deliberate source-guard test; it must not return ring production, state tests, previews, or PNG references.

**Implementation:**

Fix only removal-related failures. Do not change RADAR or BANDS behavior, `AudioAnalysisFrame`, analyzer/PCM code, playback, persistence, database code, dependencies, palette, CRT behavior, sync-offset limits, or historical approved specs/plans.

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

Confirm the diff contains only the approved ring removal, tests/assets/checklist updates, the new removal design/plan, and pre-existing uncommitted visualizer documents.

## Definition of Done

- [ ] All tasks completed in order.
- [ ] The failing three-mode tests and source guard were recorded before production cleanup.
- [ ] The display cycle is exactly ART -> RADAR -> BANDS -> ART.
- [ ] Labels are exactly `ART 1/3`, `RADAR 2/3`, and `BANDS 3/3`.
- [ ] Accessibility names remain `Album art`, `Circular radar`, and `Spectrum bars`.
- [ ] No ring enum, renderer, geometry, smoothing/echo state, tag, current test, preview, or PNG reference remains.
- [ ] RADAR, BANDS, unavailable fallback, CRT effects, deck interaction, and shared sync controls remain unchanged.
- [ ] Historical approved specs and plans remain unchanged.
- [ ] Unit tests, Android-test assembly, screenshot validation, lint, and debug build pass.
- [ ] Connected tests and Pixel 7 checks pass when a device is available, or are explicitly reported pending.
- [ ] `git diff --check` passes and no unrelated files are modified.
