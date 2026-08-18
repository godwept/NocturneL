# Visualizer Sync Offset Design

**Date:** 2026-08-18
**Status:** Approved

## Goal

Let users compensate for output latency, particularly Bluetooth latency, by applying one persisted timing offset to every PCM-driven visualizer without changing audio playback timing.

## Success Criteria

- [ ] Radar, spectrum, and oscilloscope share one visualizer sync offset.
- [ ] The offset is adjustable in 25 ms increments from -500 ms through +1000 ms.
- [ ] Positive values delay the visualization and negative values make it earlier.
- [ ] Changes take effect during playback without restarting audio or the visualizer.
- [ ] The selected offset survives app close and reopen.
- [ ] Album art and audio playback behavior remain unchanged.

## Scope

**In scope:**

- One global visualizer sync offset.
- Minus, reset/value, and plus controls along the bottom of the visualization square.
- Reset to 0 ms by tapping the displayed offset.
- Persistence through the existing terminal preferences infrastructure.
- Playback-relative PCM window selection before analysis.
- Accessibility labels for decreasing, increasing, and resetting the offset.

**Out of scope:**

- Per-headset or per-output-device offsets.
- Automatic Bluetooth latency detection.
- Changes to audio playback timing.
- Separate offsets for individual visualizer modes.
- Persisting the selected visualizer mode.

## Design

### State and Persistence

`TerminalSettingsState` gains an integer `visualizerSyncOffsetMs`, with a default of 0. `TerminalPreferencesRepository` persists the integer beside the existing effects preference. Invalid persisted values are clamped to the supported -500 through +1000 ms range when loaded.

The settings view model owns decrease, increase, and reset operations. Decrease and increase move in 25 ms increments and clamp at the supported limits. The app passes the current value and callbacks into Now Playing and forwards the value to the shared `AudioAnalysisRepository`, keeping the persisted setting as the single durable source of truth.

### PCM Alignment

The offset is applied once when selecting the playback-relative PCM window, before an `AudioAnalysisFrame` is produced. Radar, spectrum, and oscilloscope consequently receive the same synchronized frame.

The PCM sink converts the millisecond value to a sample count using the current stream sample rate. A positive offset adds samples behind the reported playback position and selects older PCM, delaying the visualization. A negative offset reduces the existing samples-behind distance and selects newer PCM, making the visualization earlier.

When a negative offset asks for samples beyond the newest PCM already decoded, selection clamps to the newest available sample instead of failing. The requested setting remains displayed and persisted. Ring-buffer bounds remain authoritative so selection cannot read overwritten or incomplete samples.

Changing the offset affects the next analysis update, normally within one analysis interval, without resetting analyzer smoothing or interrupting playback. Sample-rate changes automatically recalculate the required sample count.

### Controls and Interaction

`VisualizerDeck` receives the current offset and decrease, increase, and reset callbacks. While radar, spectrum, or oscilloscope is active, a compact overlay along the bottom of the square displays:

`[-]  VIS SYNC +150 ms  [+]`

The center value is tappable and resets the offset to 0 ms. Minus and plus become disabled at -500 ms and +1000 ms respectively. Taps on any sync control are consumed and do not cycle the deck; tapping elsewhere in the square continues cycling visualizer modes. Album-art mode hides the controls because the offset has no effect there.

Accessibility semantics announce the decrease and increase actions, the signed current offset, and the reset action.

### Error Handling and Edge Cases

- After a seek, track change, or fresh analyzer start, a positive delay may briefly leave the visualizer in its existing calm or idle state while enough PCM accumulates.
- Pausing retains the setting while analysis follows its existing stop behavior.
- Preference-write failure does not affect playback; the in-memory value remains usable for the current app session.
- Unsupported or unavailable analysis retains the existing safe fallback behavior, and the sync controls remain operable.
- Offset changes never alter, pause, seek, or otherwise manipulate audible playback.

## Testing Strategy

- Preference tests verify the 0 ms default, persistence across repository recreation, and clamping of malformed or out-of-range stored values.
- Pure adjustment tests verify 25 ms increments, both limits, and reset behavior.
- PCM sink tests use identifiable samples to prove that positive offsets select older PCM, negative offsets select newer available PCM, calculations scale with sample rate, and unavailable future PCM clamps safely.
- Analysis repository tests verify that changing the offset affects the next analysis cycle without restarting playback.
- Compose tests verify control visibility, labels, enabled states, reset behavior, and that control taps do not cycle visualizer modes.
- Screenshot coverage includes a representative active visualizer with the sync overlay.
- Existing visualizer, playback-safety, settings, and Now Playing tests provide regression coverage.
- A Pixel 7 device check calibrates wired and Bluetooth output and confirms that the selected value survives a full app restart.

## Open Questions

None.
