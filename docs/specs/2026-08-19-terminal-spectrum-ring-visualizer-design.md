# Terminal Spectrum Ring Visualizer Design

**Date:** 2026-08-19
**Status:** Approved

## Goal

Replace the visually static kaleidoscope tunnel with a clearly animated terminal spectrum ring inspired by a radial audio spectrum. The new visualizer presents a tilted perspective ellipse of outward-facing phosphor-green spikes, remains synchronized through the existing shared analysis pipeline, and preserves NocturneL's black-and-green terminal identity.

## Success Criteria

- [ ] The visualizer cycle is exactly `Album art -> Circular radar -> Spectrum bars -> Terminal spectrum ring -> Album art`.
- [ ] The fourth mode is labeled `RING 4/4` and announced as `Terminal spectrum ring`.
- [ ] A tilted elliptical ring with 64-96 radial spikes is immediately recognizable at supported visualizer sizes.
- [ ] The spectrum pattern slowly orbits during active playback and freezes when playback is paused.
- [ ] Bass, mid, high, waveform, transient, and frame-ID data each have a visible and predictable role.
- [ ] The near side reads brighter and heavier than the far side, creating convincing perspective.
- [ ] CRT effects add restrained glow and transient echo treatment without introducing additional colors.
- [ ] Silence, unavailable analysis, invalid samples, seeks, track changes, and effects changes remain bounded and safe.
- [ ] The existing shared visualizer sync offset continues to affect the ring without ring-specific settings.
- [ ] Playback remains smooth and glitch-free on a Pixel 7.

## Scope

**In scope:**

- Replacing the tunnel renderer, geometry, history, display name, accessibility name, and test tag.
- A tilted perspective ellipse made from adaptive radial spectrum spikes.
- Slow PCM-frame-driven orbit during active playback.
- Waveform-driven local spike movement, bass-driven breathing, mid-driven primary height, high-driven tip detail, and transient echo ellipses.
- Perspective-aware stroke brightness and thickness using the existing phosphor palette.
- Light temporal smoothing that preserves visible musical response.
- Existing CRT glow when effects are enabled and crisp rendering when effects are disabled.
- Unit, Compose, screenshot, regression, and Pixel 7 verification.

**Out of scope:**

- New colors, gradients, or a user-selectable palette.
- Changes to PCM capture, FFT analysis, or `AudioAnalysisFrame`.
- Beat or tempo detection.
- Independent timer animation or motion while paused.
- User controls for tilt, orbit speed, spike count, smoothing, or intensity.
- Adding a fifth visualizer mode.
- Changes to visualizer sync-offset behavior.

## Design

### Visual Language

The visualizer centers a horizontally oriented ellipse on the canvas and tilts it in perspective. Between 64 and 96 evenly distributed spikes extend outward from its rim. Each spike begins at the ellipse and follows the projected radial direction, creating the appearance of a circular spectrum viewed at an angle rather than a flat oval bar chart.

The lower, near-facing portion of the ellipse uses brighter phosphor green, slightly greater opacity, and heavier strokes. The upper, far-facing portion uses dimmer, thinner strokes. Depth is derived continuously from each spike's angular position so the transition between near and far edges is smooth.

The ring remains centered and structurally stable while its audio pattern slowly orbits around the rim. This provides continuous visible motion without rotating or wobbling the whole canvas. A dark center and generous black surrounding space preserve the terminal presentation and prevent the result from becoming visually crowded.

Only the existing `TerminalBlack`, `PhosphorMuted`, `PhosphorDim`, `Phosphor`, and `PhosphorBright` colors are used. CRT effects may add a restrained green glow, but the rainbow palette from the visual reference is deliberately not reproduced.

### Audio and Motion Mapping

The renderer consumes the existing synchronized `AudioAnalysisFrame`. Its inputs have distinct responsibilities:

- Waveform samples provide the detailed per-spike shape around the ring.
- Mid energy controls the primary outward height of the spikes.
- High energy adds shorter, sharper movement at spike tips.
- Low energy gently expands and contracts both ellipse radii so the ring breathes with bass.
- A strong transient produces one brief expanding echo ellipse.
- `frameId` supplies a bounded angular phase that slowly shifts sample placement around the ellipse.

Orbit advances only when fresh active analysis frames arrive. An unchanged frame always produces unchanged geometry, and paused or idle playback cannot drift because there is no independent animation timer.

Adjacent spike magnitudes and consecutive active frames are blended lightly to reduce harsh single-frame flicker. Smoothing is intentionally limited so percussion and vocal articulation remain obvious. State resets on non-increasing frame IDs, idle or unavailable analysis, seeks, track changes, and mode re-entry, preventing stale motion from leaking into a new playback context.

### Geometry and State

A pure ring projection converts canvas dimensions, a current analysis frame, and the bounded smoothing state into renderable geometry:

- The ellipse center and horizontal and vertical radii.
- An adaptive set of 64, 80, or 96 spike segments based on available size.
- Per-spike base point, tip point, depth, stroke emphasis, and brightness tier.
- An optional transient echo ellipse.
- A bounded orbit phase derived from `frameId`.

For each angular sample, the ellipse base point is calculated from the horizontal and compressed vertical radii. The outward direction is calculated from the projected ellipse normal, not simply from the canvas center, so spikes remain visually perpendicular to the tilted rim. Spike height is clamped as a proportion of the available canvas and all endpoints, glow strokes, and echo geometry remain inside a safety inset.

The smoothing reducer retains only the immediately previous active ring magnitudes and bounded transient state. The transient state may survive for a small fixed number of fresh analysis frames so its echo visibly expands and fades. It is not retained when CRT effects are disabled; effects-disabled rendering draws the current smoothed ring with crisp strokes and no glow or echo trail.

Empty waveform data produces a calm, evenly spaced base ring. Non-finite waveform and band values are sanitized before projection. Zero or invalid canvas dimensions return empty bounded geometry rather than attempting to draw malformed paths.

### Mode and Renderer Integration

`VisualizerDisplayMode.TUNNEL` becomes `RING`, with label `RING 4/4` and accessibility name `Terminal spectrum ring`. The mode cycle becomes:

`ART -> RADAR -> BANDS -> RING -> ART`

The renderer exposes the stable test tag `visualizer-ring`. The old tunnel branch, `TunnelGeometry`, `TunnelHistory`, tunnel constants, tunnel source guard, tunnel-specific tests, and tunnel screenshot references are removed or replaced rather than left dormant.

The selected visualizer is currently temporary Compose state, so renaming the enum requires no stored-preference migration. The visualizer sync controls remain unchanged and topmost. Because the ring consumes the same already-aligned analysis frame as RADAR and BANDS, the persisted shared Bluetooth offset applies automatically to it.

Historical approved design documents remain unchanged. This document supersedes their tunnel-specific behavior for the fourth visualizer mode.

### CRT Effects and Fallback States

With CRT effects enabled, each spike may receive a soft dim under-stroke followed by its sharper phosphor stroke. A qualifying transient draws a bright ellipse that expands and fades over a few fresh frames. The existing deck-level scanline treatment remains unchanged.

With CRT effects disabled, the ring retains its audio response, bass breathing, perspective emphasis, and slow orbit, but uses crisp strokes without glow or transient echo trails.

Idle and digital silence display a faint stable ellipse with minimal spikes and no orbit jump. Paused playback freezes the most recent motion according to existing playback-state behavior. Unavailable analysis continues to use the centered `SIGNAL UNAVAILABLE` fallback. Resume, seek, and track transition behavior begins from fresh analysis state.

## Testing Strategy

Display-mode unit tests verify the exact cycle, `RING 4/4` label, and `Terminal spectrum ring` accessibility name.

Pure geometry tests verify deterministic output, adaptive spike counts, tilted ellipse projection, outward projected normals, orbit phase progression, paused-frame stability, bass breathing, mid-height response, high-frequency tip detail, waveform distribution, depth emphasis, bounded transient echo, smoothing limits, calm empty input, non-finite input sanitization, small canvases, and bounds.

Reducer tests verify single-frame smoothing history, bounded echo lifetime, and immediate clearing on idle, unavailable analysis, effects changes, non-increasing frame IDs, and mode changes.

Compose tests verify the `visualizer-ring` tag, unavailable fallback, CRT effects behavior, effects-disabled rendering, mode cycling, accessibility semantics, and unchanged shared sync controls.

Screenshot coverage includes a representative active ring with effects enabled, the same frame with effects disabled, a quiet ring, and a strong-transient ring. Review checks the tilted perspective, near/far depth treatment, readable spike motion, clipping, glow restraint, and terminal-green palette.

The full existing analyzer, playback, visualizer, screenshot, lint, and build suites provide regression coverage. Pixel 7 verification covers smooth motion, visible response across different music, pause and resume, seeks, track changes, sync adjustment, rapid mode cycling, effects on and off, and an extended playback session without audio glitches, excessive heat, or unreasonable battery use.

## Open Questions

None.
