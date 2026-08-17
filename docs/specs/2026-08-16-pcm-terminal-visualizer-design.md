# PCM Terminal Visualizer Design

**Date:** 2026-08-16
**Status:** Approved

## Goal

Turn the square display on NocturneL's Now Playing screen into a tappable visual deck. Each visit begins with the existing album art, and successive taps cycle through three terminal-themed visualizers driven by PCM from NocturneL's own Media3 playback pipeline. The visual response must remain tightly synchronized with audible transients and frequency changes without risking playback quality.

## Success Criteria

- [ ] The display cycles exactly through `Album art -> Circular radar -> Spectrum bars -> Oscilloscope -> Album art`.
- [ ] Every visit to Now Playing begins on album art.
- [ ] All visualizer motion is derived from current playback PCM rather than elapsed-time simulation.
- [ ] Pausing, seeking, and track transitions update the visualization promptly and correctly.
- [ ] Switching display modes is immediate and never interrupts playback.
- [ ] The visualization remains smooth and playback remains glitch-free on a Pixel 7.
- [ ] All modes preserve NocturneL's black-and-green CRT terminal identity with effects enabled or disabled.
- [ ] Missing audio analysis produces a calm, readable terminal fallback instead of a crash or permission request.

## Scope

**In scope:**

- One tappable square display area on Now Playing.
- Four display modes in the agreed cycle.
- Real-time PCM capture from NocturneL's Media3 playback pipeline.
- A shared analysis model containing waveform samples, frequency-band energy, overall loudness, frequency-region energy, and transient impulses.
- Green terminal styling built from the app's existing theme tokens.
- Fast attack and controlled decay for responsive but readable movement.
- Correct behavior for play, pause, seek, track transition, silence, missing artwork, unavailable analysis, and leaving the screen.
- Accessibility semantics that identify the current mode and the result of the next tap.
- Unit, state, Compose, screenshot, and focused device testing.

**Out of scope:**

- Microphone or system-wide audio capture.
- Visualizing audio from other applications.
- Offline track analysis or cached beat maps.
- User-editable colors, sensitivity, mode ordering, or other tuning controls.
- Visualizer types beyond radar, spectrum bars, and oscilloscope.
- Persisting the selected mode.
- Full-screen visualization.
- Musical tempo or beat tracking. Transients respond to the audio signal, but semantic beat detection is a separate feature.

## Design

### Interaction and Lifecycle

The existing artwork square becomes a display deck with this tap cycle:

`Album art -> Circular radar -> Spectrum bars -> Oscilloscope -> Album art`

The selected mode is local Now Playing UI state. It initializes to album art whenever the screen is entered, is not persisted, and does not become part of playback state. A track change does not reset the mode while the user remains on Now Playing.

After a tap, a compact corner label briefly identifies the selected mode as `ART 1/4`, `RADAR 2/4`, `BANDS 3/4`, or `SCOPE 4/4`, then fades. Accessibility semantics announce the active mode and explain that tapping advances to the next mode.

### Audio Analysis State

The playback service owns audio analysis because it has direct access to decoded PCM. Compose receives lightweight, immutable snapshots and never reads or retains raw audio buffers.

Each snapshot contains:

- A normalized waveform for the oscilloscope.
- Approximately 32 logarithmic frequency bands for the spectrum and radar.
- Overall signal energy for glow and motion intensity.
- Low-, mid-, and high-frequency energy for layered movement.
- A transient impulse value for sharp musical hits.
- A monotonically increasing frame identifier for stale-frame detection.
- An active or idle status for pause, silence, startup, teardown, and unavailable analysis.

The real-time audio path copies samples into a fixed, reusable handoff buffer and immediately continues playback. A dedicated analyzer consumes the newest available samples, applies windowing and FFT analysis, and publishes snapshots at display rate. If analysis falls behind, it drops old analysis frames; audio never waits for visualization.

Attack is fast so percussion lands crisply. Decay is slower so shapes remain readable rather than flickering. The shared analysis source exists only for the app process and exposes an idle snapshot when playback is absent, paused, unsupported, or being reconfigured.

### Component Boundaries

The feature has four boundaries:

1. **PCM tap:** Receives decoded stereo or mono samples from Media3 without altering them.
2. **Audio analyzer:** Converts samples into waveform, frequency bands, energy, and transient values.
3. **Analysis source:** Publishes only the newest immutable snapshot to the app UI.
4. **Visualizer deck:** Owns the local display mode, handles taps, and draws the selected scene inside the existing square.

This separation keeps audio-thread work bounded, makes signal processing testable without Android UI, and lets each visualization render from deterministic snapshots.

### Visual Language

All modes use the app's existing black background and terminal greens. With CRT effects enabled, the deck adds scanlines, restrained phosphor bloom, and short persistence trails. With effects disabled, it uses a crisp one-pixel-style treatment without glow or trails. The visualization itself remains functional in both settings.

**Circular radar:** Concentric terminal rings and radial frequency spokes fill the square. Bass expands the inner mass, mids shape the central ring, highs sharpen the outer edge, and transients create brief outward echo rings. A thin sweep rotates only while playback advances; musical deformation comes directly from PCM.

**Spectrum bars:** Green segmented columns run from bass to treble. They rise quickly, fall more slowly, and retain subtle peak markers. Their block construction should resemble terminal glyphs rather than a contemporary neon equalizer.

**Oscilloscope:** A centered green waveform uses restrained persistence trails. Loudness controls trace intensity, while silence settles toward a nearly flat line. Stereo character may be retained by the waveform while frequency analysis uses a predictable mono mix.

**Album art:** The current CRT artwork presentation remains unchanged. If artwork is missing, the existing terminal placeholder remains the first display mode.

### Performance and Lifecycle Rules

- Playback always takes priority over visualization.
- The audio-thread handoff performs no blocking, file access, logging, or per-buffer allocation.
- FFT work runs only while a visualizer mode is visible; album art mode and other screens leave heavy analysis dormant.
- Entering a visualizer begins with fresh PCM rather than replaying stale samples collected while artwork was visible.
- If the analyzer falls behind, it drops visual frames rather than buffering them.
- When the UI stops consuming analysis, processing shuts down cleanly and releases its working buffers.

### Error Handling and Edge Cases

- Pause smoothly settles the current shape into its idle form; resume reacts to the first new PCM frame.
- Seeking and track transitions clear waveform history, peak holds, transient trails, and smoothing state immediately.
- Sample-rate, channel-count, or PCM-format changes rebuild analyzer state safely between buffers.
- Mono input is displayed symmetrically. Stereo input is mixed predictably for frequency analysis while the scope may retain left/right character.
- Digital silence produces a stable, faint terminal baseline rather than random motion.
- If analysis is unavailable, a selected visualizer shows a quiet `SIGNAL UNAVAILABLE` display and mode cycling continues to work.
- No visualization failure may stop playback, crash the media service, or trigger a user permission request.

## Testing Strategy

Signal-processing tests use deterministic synthetic PCM:

- Silence produces zero energy, a flat waveform, and no transient.
- Pure low-, mid-, and high-frequency tones activate the expected bands.
- An impulse creates an immediate transient followed by the specified decay.
- Stereo and mono inputs normalize correctly.
- Format changes, seeks, pauses, and track resets clear prior history.
- PCM leaving the analysis tap is sample-for-sample identical to PCM entering it.
- Analyzer overload drops frames without blocking the producer.

State tests verify the exact mode cycle, album-art initialization on every screen entry, mode retention across track changes during the same visit, and deterministic idle and unavailable states.

Compose tests verify tap behavior and accessibility descriptions. Screenshot tests render fixed analysis snapshots rather than live audio and cover all modes, CRT effects on and off, silence, strong bass, a transient, and unavailable signal.

A focused Pixel 7 device check verifies glitch-free playback during rapid mode switching, smooth animation through long tracks and playback transitions, perceptual alignment with obvious percussion, and reasonable CPU, heat, and battery behavior during an extended session.

## Open Questions

None. Decisions about exact FFT size, snapshot rate, smoothing constants, and buffer sizes are implementation details to be selected and validated against the approved behavior and Pixel 7 performance criteria.
