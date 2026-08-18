# Kaleidoscope Tunnel Visualizer Design

**Date:** 2026-08-18
**Status:** Approved

## Goal

Replace NocturneL's minimal oscilloscope with a restrained, psychedelic kaleidoscope tunnel that remains unmistakably part of the black-and-green terminal interface. The tunnel will respond deterministically to the existing PCM analysis data, provide more visual depth and musical character, and preserve smooth, glitch-free playback.

## Success Criteria

- [ ] The visualizer cycle is exactly `Album art -> Circular radar -> Spectrum bars -> Kaleidoscope tunnel -> Album art`.
- [ ] The fourth mode is labeled `TUNNEL 4/4` and announced as `Kaleidoscope tunnel`.
- [ ] Nested four-way symmetric tunnel frames create clear depth without aggressive motion.
- [ ] Waveform, bass, mid, high, transient, and frame-ID data each contribute visibly and predictably to the scene.
- [ ] All animation remains derived from new PCM analysis frames rather than an independent timer.
- [ ] Silence, pause, seeks, track changes, unavailable analysis, and effects changes remain calm and safe.
- [ ] The tunnel preserves NocturneL's existing black and phosphor-green terminal palette.
- [ ] Playback remains smooth and glitch-free on a Pixel 7.

## Scope

**In scope:**

- Replacing the existing oscilloscope renderer and geometry.
- Renaming the fourth display mode, label, accessibility name, and test tag.
- Four-way symmetric nested polygon frames with restrained rotation and depth travel.
- Subtle waveform-driven edge deformation.
- Bass-driven layer spacing and pulse, mid-driven corner bending, high-driven edge detail, and one transient echo layer.
- Existing phosphor colors, glow, scanlines, and short persistence with CRT effects enabled.
- Crisp, trail-free rendering with CRT effects disabled.
- Deterministic geometry, Compose, screenshot, regression, and Pixel 7 verification.

**Out of scope:**

- New colors or a user-selectable palette.
- Changes to PCM capture, FFT analysis, or `AudioAnalysisFrame`.
- Stereo phase or Lissajous data.
- Beat detection, tempo analysis, or independent timer animation.
- Adding another visualizer slot; the tunnel replaces scope in the existing cycle.
- User controls for tunnel speed, symmetry, intensity, or geometry.
- Changes to visualizer sync-offset behavior.

## Design

### Visual Language and Musical Mapping

The tunnel uses a fixed central vanishing point and several nested four-sided frames. The frames recede inward with slow PCM-frame-driven travel and restrained rotation. Their structure remains readable at ordinary listening levels instead of continuously folding or flashing.

The existing normalized waveform is folded and mirrored across all four sides of every depth layer. This creates organic edge deformation while preserving four-way balance. The deformation remains subtle enough that the stable polygon tunnel is always recognizable.

Existing analysis values control distinct visual properties:

- Bass adjusts layer spacing and the overall depth pulse.
- Mid energy bends the polygon corners.
- High energy introduces small edge ripples and fine detail.
- A transient creates one brief bright echo frame moving outward from the tunnel.
- `frameId` controls bounded rotation and inward depth phase, so an unchanged analysis frame produces unchanged geometry.

The palette remains `TerminalBlack` plus the existing muted, dim, normal, and bright phosphor greens. Psychedelic character comes from symmetry, depth, deformation, brightness, and persistence rather than added colors.

### State and Geometry

No playback, persistence, PCM, FFT, or analysis-model changes are required. A pure `TunnelGeometry` projection converts an `AudioAnalysisFrame` and canvas dimensions into:

- A fixed central vanishing point.
- Several nested polygon layers.
- Symmetrically deformed edge points for each layer.
- One optional transient echo layer.
- Bounded rotation and depth phase derived from `frameId`.

Geometry generation handles missing waveform or band values by producing symmetric undeformed layers. Layer count and edge detail reduce gracefully on small canvases. Every generated point, including the transient echo, remains clipped within the square.

With CRT effects enabled, Compose retains no more than three prior immutable analysis frames and reconstructs their tunnel geometry at the current canvas size. These frames draw first with muted, decreasing opacity, creating short phosphor persistence. Effects-disabled rendering uses only the current frame with crisp strokes.

History clears when leaving tunnel mode, entering idle or unavailable state, disabling effects, receiving a non-increasing frame ID, seeking, changing tracks, or restarting analysis.

### Mode and Renderer Integration

`VisualizerDisplayMode.SCOPE` becomes `TUNNEL`, with label `TUNNEL 4/4` and accessibility name `Kaleidoscope tunnel`. The exact mode cycle becomes:

`ART -> RADAR -> BANDS -> TUNNEL -> ART`

The renderer exposes the stable test tag `visualizer-tunnel`. It replaces the oscilloscope branch with tunnel geometry and drawing. Old tunnel-history layers render first, followed by the current nested frames and then the optional bright transient echo.

The obsolete scope geometry, waveform-path renderer, scope history, scope tag, and current scope screenshot references are removed rather than left dormant. The existing visualizer sync controls and temporary mode label remain topmost, visible, and unchanged.

Historical approved design documents remain unchanged. This document supersedes their oscilloscope-specific behavior for the fourth visualizer mode.

### Error Handling and Edge Cases

- Idle and digital silence show a faint, evenly spaced, centered tunnel without a transient echo or rotation jump.
- Pause follows the existing idle behavior; resume responds to fresh PCM rather than replaying stale persistence.
- Seeks, track changes, analysis resets, effects changes, and mode changes clear tunnel history immediately.
- Unavailable analysis continues to show the centered `SIGNAL UNAVAILABLE` fallback.
- Empty waveform or band collections produce balanced undeformed layers.
- Small or zero-sized canvases return bounded minimal geometry without inverted paths or crashes.
- Rotation and depth use bounded phases to avoid precision drift during long sessions.
- Transients brighten one echo frame but never flash the entire screen.
- The restrained profile avoids rapid zooms, reversals, or high-frequency whole-scene rotation.
- Visualizer sync controls continue to affect the shared analysis frame before rendering and require no tunnel-specific logic.

## Testing Strategy

Display-mode unit tests verify the exact cycle, `TUNNEL 4/4` label, and `Kaleidoscope tunnel` accessibility name.

Pure geometry tests verify four-way symmetry, point bounds, deterministic output, bounded phase behavior, PCM-frame-driven movement, energy mappings, mirrored waveform deformation, exactly one bounded transient echo, calm silence, empty inputs, and small canvas dimensions.

Compose tests verify the `visualizer-tunnel` tag, unavailable fallback, CRT effects behavior, short persistence and history clearing, mode cycling, accessibility semantics, and unchanged visualizer sync controls.

Screenshot coverage replaces the two scope references with a deterministic restrained tunnel with CRT effects enabled, the same frame with effects disabled, and a strong-transient tunnel for reviewing the echo treatment.

Existing analyzer, PCM alignment, sync-offset, playback-safety, and other visualizer tests run for regression coverage. No audio-analysis expectations should change.

The Pixel 7 checklist verifies restrained visual comfort, smooth motion, pause and resume, seeks, track transitions, sync adjustment, rapid mode cycling, effects on and off, and an extended session without audio glitches, excessive heat, or unreasonable battery use.

## Open Questions

None.
