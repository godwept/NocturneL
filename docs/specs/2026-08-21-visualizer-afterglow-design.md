# Visualizer Afterglow Design

**Date:** 2026-08-21
**Status:** Approved

## Goal

Add restrained CRT-style persistence to both active visualizer modes while preserving their crisp terminal appearance. Radar gains a short trail behind only its sweeping arm, while spectrum bars gain falling ghost segments that briefly retain recent higher values.

## Success Criteria

- [ ] Radar shows a short, fading trail behind its bright sweeping arm; the grid, energy rings, spokes, and transient echo do not trail.
- [ ] Bands retain recent higher segments as dim ghost segments that fall and fade for about 250 ms.
- [ ] Live visualizer elements remain brighter than all afterglow layers.
- [ ] Disabling effects, losing the signal, pausing playback, or leaving a VIS mode clears its history immediately.
- [ ] The effect remains smooth and bounded without changing audio analysis, synchronization, mode selection, or layout.

## Scope

**In scope:**

- A short phosphor trail behind the radar sweep arm.
- Falling, fading ghost segments for each spectrum bar.
- Time-based decay targeting roughly 250 ms, independent of display refresh rate.
- Immediate history reset when effects are disabled, analysis is inactive or unavailable, or the visualizer mode changes.
- Focused geometry and state tests plus visualizer UI coverage.

**Out of scope:**

- Trails on radar rings, spokes, transient echoes, or spectrum peak markers.
- User-adjustable glow intensity or duration settings.
- Bloom, blur, shaders, or changes to the existing color palette.
- Changes to PCM analysis, synchronization, navigation, layout, or the scanline effect.

## Design

### Transient State

The afterglow uses presentation-only state owned by `TerminalVisualizerScene`; nothing is persisted or added to the audio frame model.

- Radar keeps a small bounded sequence of recent sweep angles and their elapsed ages. Entries older than 250 ms are discarded, and opacity decreases with age.
- Bands keep one decay envelope per band containing the retained segment height and its age. A new taller live value raises the envelope immediately; when the live bar falls, the envelope descends toward it while fading.
- Elapsed time comes from Compose's monotonic animation frame clock so decay remains consistent across different display refresh rates.
- State is recreated or cleared when the mode changes, effects turn off, or `AnalysisStatus` is anything other than `ACTIVE`.
- Radar samples and band envelopes remain fixed-size, preventing history or allocation growth during long playback.

### Interfaces and Rendering

The existing `TerminalVisualizerScene(mode, frame, effectsEnabled, modifier)` interface remains unchanged. Internally, a small pure decay model accepts the current frame values plus elapsed time and returns drawable afterglow geometry.

- Radar draws the oldest trail arms first, from dimmest to brightest, then draws the live sweep arm last.
- Bands draw ghost segments first, using dim phosphor and decreasing opacity, then draw live segments and the current peak marker over them.
- A single internal 250 ms decay constant governs both modes.
- Afterglow drawing runs only for an active signal with effects enabled. Otherwise, the ordinary visualization remains and retained state is cleared.
- Colors reuse the existing phosphor theme tokens; no new public component, setting, callback, or audio-layer API is introduced.

### Error Handling and Edge Cases

- The first active frame renders only live content; it has no invented trail.
- Radar angle wraparound at 360 degrees follows the shortest backward trail and never flashes across the circle.
- Large frame-time gaps clear expired history instead of producing a long jump or oversized decay step.
- A change in band count safely rebuilds the band envelopes.
- Canvas size changes discard retained geometry so ghosts cannot appear at stale coordinates.
- `IDLE` and `UNAVAILABLE` frames clear afterglow immediately; the existing unavailable message remains unchanged.
- Turning effects off or switching among ART, RADAR, and BANDS clears all retained state immediately.
- Zero-height bars produce no live segments; only a valid unexpired ghost may remain while the signal is active.
- Afterglow is decorative and adds no semantics, controls, or accessibility announcements.

## Testing Strategy

Pure unit tests will verify:

- Radar samples expire at 250 ms, fade monotonically, remain bounded, and handle 360-degree wraparound.
- Bar envelopes rise immediately, then fall and fade over time without dropping below the live height.
- Large time gaps, band-count changes, and explicit resets clear state safely.
- Disabled effects and non-active analysis never produce afterglow geometry.

Compose and UI tests will verify:

- Existing radar and bands tags, sizing, and non-clickable behavior remain intact.
- Scanlines and afterglow both follow the effects setting.
- `UNAVAILABLE` still displays its existing message without retained glow.
- Mode changes do not carry glow from one visualizer into another.

A focused screenshot/reference check will confirm that the live sweep and bars remain visually dominant and the glow stays short and restrained.

## Open Questions

None.
