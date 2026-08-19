# Visualizer Sync Layout Design

**Date:** 2026-08-19
**Status:** Approved

## Goal

Make the visualizer synchronization controls easy to read without covering the active visualization. Remove the `[ NOW PLAYING ]` caption and place the existing VIS SYNC controls in a centered row immediately above the square display whenever a visualizer mode is active, while preserving the display's current square dimensions.

## Success Criteria

- [ ] The outer terminal frame remains, but it no longer displays `[ NOW PLAYING ]`.
- [ ] Album art, circular radar, and spectrum bars retain their existing square dimensions.
- [ ] Album-art mode shows no VIS SYNC row and reserves no empty space for it.
- [ ] Radar and spectrum modes show the existing full VIS SYNC controls centered immediately above the square.
- [ ] Minus, plus, and reset behavior remains unchanged.
- [ ] Tapping the square cycles display modes, while operating VIS SYNC does not cycle the display.

## Scope

**In scope:**

- Remove the title from the Now Playing `AsciiFrame`.
- Restructure `VisualizerDeck` as a vertical section containing the conditional VIS SYNC row and the square display.
- Preserve the current control wording, touch targets, enabled and disabled limits, persistence, and accessibility descriptions.
- Update Compose and screenshot coverage for album-art and active-visualizer layouts.

**Out of scope:**

- Changing visualizer rendering, synchronization behavior, or offset limits.
- Changing the remaining Now Playing metadata and playback controls.
- Adding VIS SYNC to album-art mode or another screen.
- Redesigning the shared `AsciiFrame`.

## Design

### State and Component Boundaries

`VisualizerDeck` continues to own its local display mode. It derives `visualizerActive` as it does today: circular radar and spectrum bars are active visualizer modes, while album art is not.

When a visualizer is active, the deck renders a centered VIS SYNC row followed by the square display. In album-art mode, the row is not composed, so it leaves no placeholder gap. The existing offset value and callbacks continue to flow from `NowPlayingScreen`; this change introduces no new persisted state.

### Layout and Interfaces

`NowPlayingScreen` calls `AsciiFrame` without a title and gives `VisualizerDeck` the available width rather than applying a square aspect ratio to the entire deck.

Within `VisualizerDeck`:

- The outer container occupies the available width.
- VIS SYNC is centered above the display only while `visualizerActive` is true.
- The display box independently uses a `1:1` aspect ratio, preserving its current dimensions.
- Transient display-mode labels remain overlaid inside the square.
- Only the square is the mode-cycling tap target, so VIS SYNC interactions cannot change modes.

The existing parameters and callback contracts remain intact. The deck modifier describes the whole deck section instead of only the square display.

### Error Handling and Edge Cases

- On narrow screens, the existing VIS SYNC controls remain centered on their own row, without competing with a header caption.
- At the minimum or maximum offset, the corresponding decrement or increment control remains disabled.
- Tapping the center control continues to reset the offset.
- Selecting a visualizer adds the row above the unchanged square; returning to album art removes it immediately.
- Idle or unavailable analysis still shows VIS SYNC because visibility follows the selected display mode rather than signal availability.
- Removing the caption does not remove the frame or alter existing visualizer and control accessibility descriptions.

## Testing Strategy

- Add or update a Compose test confirming album-art mode has no VIS SYNC row.
- Switch to radar and verify the row appears above, rather than inside, the square and that all three controls work without changing modes.
- Verify the row remains in spectrum mode and disappears after cycling back to album art.
- Retain coverage for disabled decrement and increment controls at the offset limits.
- Add a layout assertion that the active visualizer square retains equal width and height while VIS SYNC occupies separate space above it.
- Update the deterministic Now Playing and visualizer screenshot reference, then inspect the 412dp target for clipping, alignment, and spacing.
- Run the focused visualizer Compose tests, screenshot tests, and relevant Now Playing tests.

## Open Questions

None.
