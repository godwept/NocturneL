# Adaptive Spectrum Balancing Design

**Date:** 2026-08-16
**Status:** Approved

## Goal

Enhance the spectrum analyzer so it remains visually active across the full width without losing the recognizable balance of the music. Quiet material should expand to make useful use of the display, upper-frequency content should remain visible, and true silence should still settle to a calm baseline.

## Success Criteria

- [ ] Quiet music automatically expands to use more vertical space.
- [ ] Upper-frequency bars are visibly active when treble content exists.
- [ ] Bass still appears stronger when the recording is bass-heavy.
- [ ] Adjacent columns move as a cohesive spectrum rather than a jagged collection.
- [ ] Sudden hits remain responsive without excessive flicker.
- [ ] True silence settles to a stable baseline instead of amplifying noise.

## Scope

**In scope:**

- One shared adaptive gain applied across all spectrum bands.
- A gentle, capped frequency compensation curve favoring upper bands.
- Light smoothing between neighboring bands.
- Fast response to rising energy and slower gain release.
- A silence threshold that returns the display to a calm baseline.
- Resetting adaptive state after seeks, track changes, and analyzer restarts.

**Out of scope:**

- Independent normalization for every band.
- User-adjustable sensitivity or equalizer controls.
- Changes to the actual audio output.
- Beat detection or genre-specific profiles.
- Redesigning the spectrum renderer or terminal styling.

## Design

### Analysis Pipeline

The analyzer will retain three small pieces of spectrum-only state:

- A global level envelope tracking recent spectrum strength.
- A slowly changing gain value derived from that envelope.
- A reusable working array for frequency compensation and neighbor smoothing.

Each analyzed frame will pass through this signal path:

`FFT bands -> treble compensation -> neighbor smoothing -> global auto-gain -> existing attack/release smoothing -> rendered bands`

The shared adaptive gain preserves the relationships between frequency bands. It expands quiet passages without making every column equally strong. Upper bands receive a gentle frequency-dependent lift to counter the natural spectral roll-off of typical music. The lift is capped so high-frequency noise and isolated treble components cannot dominate the display.

Light neighbor smoothing blends each interior band with the bands on either side. Edge bands use only their available neighbor. This reduces abrupt column-to-column discontinuities while retaining meaningful spectral peaks.

When signal energy falls below the silence threshold, target bands become zero and the existing release behavior settles them smoothly. The silence gate does not allow near-silent decoder noise to raise the automatic gain.

### State and Lifecycle

The global level envelope responds quickly when signal strength rises so a sudden loud section cannot saturate the spectrum. It releases more slowly when signal strength falls, allowing quiet passages to expand without rapid visible pumping.

Resetting the analyzer clears the learned level, adaptive gain, compensated working values, and existing smoothing history. Seeks, track changes, format changes, and analyzer restarts therefore cannot carry gain state from an earlier signal into a fresh one.

### Interfaces and Configuration

No public API or analysis data-model changes are required. The behavior remains encapsulated within `AudioAnalyzer`; `analyze()` and `reset()` retain their existing contracts. `AudioAnalysisFrame`, the playback audio path, waveform data, transient detection, and visualizer rendering interfaces remain unchanged.

The analyzer will use named, grouped internal constants for:

- Maximum automatic gain.
- Gain attack and release rates.
- Silence threshold.
- Upper-frequency compensation strength and cap.
- Neighbor-smoothing weights.

These are fixed implementation parameters rather than user settings. They can be calibrated using deterministic tests and focused device listening.

### Error Handling and Edge Cases

- Digital silence produces zero target bands and cannot raise automatic gain.
- Near-silent decoder noise remains below the silence threshold.
- A sudden loud section reduces gain quickly enough to prevent widespread clipping.
- Gain rises gradually during quiet sections to avoid visible pumping.
- Frequency compensation remains capped.
- Every intermediate and published value must remain finite and clamped to `0..1`.
- Reset clears all adaptive and spectral history.

## Testing Strategy

Deterministic analyzer tests will verify:

- A quiet multitone signal expands substantially more than it does under fixed normalization.
- A pink-spectrum fixture produces useful activity across both halves of the display.
- Treble compensation improves upper-band visibility without making it dominate bass.
- Neighbor smoothing reduces isolated discontinuities while retaining frequency peaks.
- Quiet and loud versions of the same signal preserve their relative spectral shape.
- A sudden loud frame reduces adaptive gain without clipping published bands.
- Digital silence and sub-threshold noise settle to zero.
- Reset removes all learned gain and spectral history.
- All intermediate and published values remain finite and within `0..1`.

Existing analyzer, geometry, playback-safety, and visualization tests will run to detect regressions. A focused device check using bass-heavy, balanced, quiet, and treble-rich tracks will confirm that the spectrum feels lively without appearing artificially flat.

## Open Questions

None. Exact gain rates, thresholds, compensation strength, cap, and neighbor weights are implementation parameters to be selected through test-first calibration.
