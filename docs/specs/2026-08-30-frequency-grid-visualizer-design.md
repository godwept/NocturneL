# Frequency Grid Visualizer Design

**Date:** 2026-08-30
**Status:** Approved

## Goal

Add a fourth visualizer that presents the existing 32 frequency bands as fixed, artistically scattered hotspots on a dense square cell grid. Each band's activity controls local brightness, nearby influences blend, and fading follows the existing CRT Effects behavior.

## Success Criteria

- [ ] Music produces distinct, blended hotspots across a dense square grid.
- [ ] Each frequency always occupies the same fixed location.
- [ ] Rendering remains responsive and visually consistent with the existing visualizers.
- [ ] Existing visualizer behavior remains unchanged.
- [ ] Automated tests pass and a debug APK builds successfully.

## Scope

**In scope:**

- A fourth tap-cycled visualizer mode.
- A dense, responsive square grid with square cells and consistent gaps.
- Fixed, artistically scattered anchors for the existing 32 frequency bands.
- Radial brightness falloff and blended overlapping hotspots.
- Existing palette, audio frames, sync controls, unavailable-signal behavior, reduced-motion handling, and CRT-controlled afterglow.
- Automated tests and a debug APK build.

**Out of scope:**

- New audio analysis or settings.
- User-configurable layouts.
- Animated hotspot positions.
- Database or persisted-state changes.
- Play Store assets, metadata, or release work.

## Design

### Data and State

- Reuse the 32 normalized values in `AudioAnalysisFrame.bands`.
- Define 32 deterministic anchor coordinates normalized to the square canvas.
- Derive the dense grid from the measured canvas size while keeping cells square and gaps consistent.
- Calculate each cell's brightness from the blended, clamped radial influence of nearby active bands.
- Retain only the prior grid brightness required for afterglow; do not persist visualizer state.

### Interfaces

- Add `GRID("GRID 4/4", "Frequency grid")` to `VisualizerDisplayMode` and update the complete mode cycle.
- Render the grid through the existing visualizer scene and `AudioAnalysisFrame` interface.
- Add focused pure functions for grid geometry, hotspot influence, blending, and afterglow calculations.
- Reuse the existing tap navigation and visualizer sync controls without adding controls.

### Behavior and Edge Cases

- Idle audio produces a dark grid.
- An unavailable signal uses the existing `SIGNAL UNAVAILABLE` treatment.
- Invalid band values are clamped or treated as zero.
- Small or non-square measurements produce a centered square grid without clipping.
- With CRT Effects disabled or reduced motion enabled, the grid shows current activity without animated afterglow.
- Mode changes, size changes, and frame rewinds clear stale afterglow.

## Testing Strategy

- Unit-test deterministic anchors, square grid geometry, radial falloff, overlap blending, clamping, and afterglow clearing and fading.
- Update mode-cycle and accessibility-label tests for four modes.
- Add rendering and wiring coverage for the grid branch.
- Run the relevant unit tests followed by the full test suite.
- Build a debug APK for device evaluation.

## Open Questions

None.
