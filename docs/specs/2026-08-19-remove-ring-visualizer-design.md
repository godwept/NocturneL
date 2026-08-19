# Remove Ring Visualizer Design

**Date:** 2026-08-19
**Status:** Approved

## Goal

Remove the Terminal Spectrum Ring completely and simplify NocturneL's display cycle to album art plus the two retained visualizers. The resulting cycle is exactly `ART 1/3 -> RADAR 2/3 -> BANDS 3/3 -> ART`, with no dormant ring implementation left in active application code or tests.

## Success Criteria

- [ ] The display cycle is exactly `ART -> RADAR -> BANDS -> ART`.
- [ ] The three labels are `ART 1/3`, `RADAR 2/3`, and `BANDS 3/3`.
- [ ] No RING mode appears in the UI or accessibility output.
- [ ] Ring production code, state, tests, previews, and screenshot references are removed.
- [ ] RADAR, BANDS, shared visualizer sync controls, CRT effects, unavailable-signal handling, and playback behavior remain unchanged.
- [ ] Unit tests, Android-test compilation, screenshot validation, lint, and the debug build pass.

## Scope

**In scope:**

- Removing `VisualizerDisplayMode.RING` and changing BANDS to cycle directly to ART.
- Relabeling every retained display mode from `/4` to `/3`.
- Removing ring geometry, smoothing and echo state, renderer code, tests, previews, and all four ring screenshot references.
- Updating active source guards and Compose/deck tests for the three-mode contract.
- Updating the Pixel 7 checklist to describe only RADAR and BANDS.

**Out of scope:**

- Replacing RING with another visualizer.
- Changing RADAR or BANDS visuals.
- Changes to PCM capture, audio analysis, shared visualizer sync offset, CRT effects, or playback behavior.
- Changes to persistence or stored data.
- Editing historical approved design or plan documents that record earlier visualizer decisions.

## Design

### Display Cycle and Labels

`VisualizerDisplayMode` contains only `ART`, `RADAR`, and `BANDS`. Its explicit cycle becomes:

`ART -> RADAR -> BANDS -> ART`

The labels become `ART 1/3`, `RADAR 2/3`, and `BANDS 3/3`. Existing accessibility names remain `Album art`, `Circular radar`, and `Spectrum bars`.

Visualizer selection remains temporary Compose state and starts at ART whenever the deck is recreated, so removing the enum entry requires no stored-data migration or compatibility mapping.

### Production Cleanup

The RING renderer branch and `visualizer-ring` test tag are removed from `TerminalVisualizerScene`. Its mode and rendering switches remain exhaustive across ART, RADAR, and BANDS. The existing terminal background, border, scanlines, unavailable `SIGNAL UNAVAILABLE` fallback, and retained radar/bands drawing code remain unchanged.

`RingState`, `RingEchoState`, their reducer, `RingGeometry`, `RingSpike`, `RingEcho`, `ringGeometry`, magnitude helpers, and all ring-only constants are deleted rather than hidden or retained for future use. Radar and spectrum geometry remain in their current shared geometry source.

The visualizer deck continues treating every non-art mode as active. Sync controls remain hidden on ART and visible on RADAR and BANDS, and their clicks do not advance the display mode.

### Tests, Previews, and Documentation

Active unit and Compose tests are rewritten around the three-mode cycle. Ring-specific state and geometry tests are removed. A source guard rejects current application references to the RING enum, renderer, geometry, state, tag, and accessibility text.

The four ring screenshot previews and their four reference PNGs are removed. Existing RADAR, BANDS, sync-controls, and unrelated screenshot references remain unchanged.

The Pixel 7 checklist is updated so mode cycling, transient alignment, sync adjustment, and long-running visualizer checks refer only to RADAR and BANDS. Ring-specific orbit, echo, smoothing, and visual-comfort checks are removed.

Historical approved specs and plans remain unchanged because they document decisions at the time they were made. This design supersedes their ring-specific active behavior.

### Error Handling and Edge Cases

- BANDS always advances directly to ART, including during rapid tapping.
- Leaving and recreating the deck continues to start at ART.
- Unavailable analysis continues to display `SIGNAL UNAVAILABLE` in RADAR and BANDS.
- Sync controls remain within their existing limits and never change display mode.
- Removing ring state cannot change PCM capture, analysis, playback, or persisted sync-offset behavior.
- If no Android device is attached, connected and Pixel 7 verification is reported as pending rather than treated as a local test failure.

## Testing Strategy

Display-mode unit tests verify the exact three-entry order, explicit cycle, `/3` labels, and retained accessibility names.

Geometry unit tests retain RADAR and BANDS coverage while all ring-specific geometry cases are deleted. Ring state tests are deleted, and the source guard verifies that no current application source contains the removed enum, geometry, state, tag, or accessibility text.

Compose tests verify RADAR/BANDS tags, unavailable fallback, scanlines behavior, three-mode deck cycling, ART reset, and unchanged sync-control behavior. Screenshot validation confirms that removing the ring previews does not alter retained references.

The complete unit, Android-test assembly, screenshot, lint, and debug build suites provide regression coverage. Connected Compose tests and the updated Pixel 7 checklist run when a device is available.

## Open Questions

None.
