# Visualizer Sync Overlay Design

**Date:** 2026-08-20
**Status:** Approved

## Goal

Keep album art, radar, and spectrum bars in exactly the same square position while making visualizer synchronization easy to adjust without permanently covering the visuals.

## Success Criteria

- [ ] Switching modes never moves or resizes the square.
- [ ] Visualizer modes show a minus control at top-left and a plus control at top-right.
- [ ] Entering a visualizer briefly shows the current `VIS SYNC` offset at top-center.
- [ ] Any sync adjustment or reset restarts the label's three-second visibility period.
- [ ] The temporary sync label is tappable and resets the offset to `0 ms`.
- [ ] The transient mode label appears at bottom-right.
- [ ] The supported range is `-2000 ms` through `+2000 ms` in `25 ms` steps.
- [ ] Album-art mode shows no sync controls.
- [ ] Sync-control taps never cycle the visualization mode.

## Scope

**In scope:**

- Replace the conditional row above the square with controls overlaid inside the square.
- Preserve the square's current dimensions and position across all display modes.
- Show corner sync buttons only in radar and spectrum modes.
- Add the transient, tappable sync-value label and relocate the transient mode label.
- Expand both sync limits to `-2000 ms` and `+2000 ms`.
- Preserve persistence, shared offset behavior, live updates, accessible labels, and adequate touch targets.

**Out of scope:**

- Sync controls on album art.
- Per-device, per-vehicle, or per-visualizer offsets.
- Automatic Bluetooth-latency detection.
- Changes to audio playback timing or visualizer rendering.
- Changes to the `25 ms` adjustment step.
- Increasing the PCM history buffer; the selected range fits its normal 48 kHz capacity.

## Design

### State and Behavior

The persisted visualizer sync offset remains the single shared value used by both visualizers. Its valid range changes to `-2000..+2000 ms`; existing saved values remain valid and require no migration.

`VisualizerDeck` retains its local display-mode state and adds local transient visibility state for the sync label. Selecting either radar or spectrum shows the current value and starts a three-second timer. Pressing minus or plus updates the offset, shows the resulting value, and restarts the timer. Tapping the visible label resets the value to `0 ms` and restarts the timer. Rapid interactions cancel and restart the previous timeout so the label remains readable until three seconds after the latest action.

Returning to album art immediately removes all sync controls and transient sync state. Re-entering a visualizer shows the persisted current value again. The display mode remains local and non-persisted.

### UI Interfaces and Interaction

`VisualizerDeck` keeps its existing inputs: the current offset plus decrease, increase, and reset callbacks. No new application-level state or playback interface is required.

An active visualizer square contains:

- A minus button at top-left, disabled at `-2000 ms`.
- A transient `VIS SYNC +/-N MS` reset label at top-center.
- A plus button at top-right, disabled at `+2000 ms`.
- The existing transient mode label at bottom-right.
- The existing mode-cycling tap target throughout the remaining square area.

Each control has its own accessible touch target and consumes taps so it cannot accidentally cycle modes. Accessibility semantics announce the decrease and increase actions, current offset, disabled limit state, and reset action. Once the value label disappears, either corner button reveals it again.

The square remains the same size and occupies the same position in every mode. Album-art mode does not compose any sync controls.

### Error Handling and Edge Cases

- Persisted values outside the new range are clamped to the nearest limit; existing in-range values remain unchanged.
- At either limit, the corresponding corner button is disabled and further taps have no effect.
- If analysis is unavailable, paused, seeking, or buffering, controls remain usable and the chosen offset remains persisted.
- A large positive delay may briefly leave the visualizer in its existing idle state after a seek or track change while sufficient PCM history accumulates.
- Negative offsets clamp analysis to the newest available PCM and never request future audio or affect playback.
- The transient label always displays the clamped, persisted value.
- Leaving a visualizer cancels its fade timer so no stale sync label appears over album art.
- Sync and mode labels occupy separate regions and fade independently.
- At uncommon sample rates above 48 kHz, the existing fixed buffer may provide less than the full two seconds of usable history. Supporting the full range for high-resolution sources would require a later buffer-capacity change.

## Testing Strategy

- Pure unit tests verify the new limits, `25 ms` stepping, clamping, labels, and reset behavior.
- Persistence tests verify both new extremes survive repository recreation and out-of-range stored values clamp correctly.
- Compose tests verify identical square bounds in album-art, radar, and spectrum modes.
- Compose tests verify that controls are absent over album art and occupy the expected corners in visualizer modes.
- Compose timing tests verify the current value appears on entry, disappears after three seconds, reappears after adjustment, and remains visible for three seconds after rapid interactions.
- Compose interaction tests verify reset, limit-disabled states, mode-label placement, and that control taps do not change display modes.
- Screenshot coverage verifies the overlay at a representative nonzero offset and checks legibility without obscuring the primary visualization.
- Existing visualizer, playback-alignment, settings, and Now Playing tests provide regression coverage.
- A Pixel 7/device check confirms that switching modes causes no vertical movement and that the expanded range is sufficient with the target vehicle's Bluetooth output.

## Open Questions

None.
