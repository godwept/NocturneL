# Full Screen Visualizers Design

**Date:** 2026-09-23

**Status:** Approved

## Goal

Let listeners expand the NOW display into an immersive, portrait visualization. The expanded view shows only one of the three audio visualizers until a tap briefly reveals an Exit control. The existing audio analysis, theme, and effects continue to drive the display.

## Success Criteria

- [ ] A long press on the NOW display opens the expanded view in its current audio visualization, or in Radar when album art is selected.
- [ ] Horizontal swipes cycle Radar, Spectrum, and Frequency Grid in that order, wrapping back to Radar.
- [ ] A tap reveals Exit, which hides automatically after about three seconds. Exit and Android Back return to NOW with the last full screen mode selected.
- [ ] The expanded view hides the app chrome and Android system bars while open, then restores them on exit.
- [ ] Radar and Grid remain centered squares, with a soft audio pulse glow in the unused space above and below. Spectrum uses the available portrait height.
- [ ] Audio analysis remains active during entry, display, and exit without an avoidable interruption.
- [ ] Automated tests pass, and the layout and system bar behavior are verified on an Android device or emulator.

## Scope

**In scope:**

- An expanded view in the existing Compose activity, presented in place of the normal app scaffold.
- Long press entry from any NOW display mode; horizontal swipe mode changes; tap to reveal Exit; Exit and Back to close.
- Immersive system bar handling, accessible entry and exit actions, and restoration of the normal screen.
- Reuse of the current visualizer scenes, audio frames, color palette, saved sync offset, CRT setting, and reduced motion behavior.
- A soft glow outside the centered Radar and Grid squares, driven by the existing transient audio value.
- Tests for interaction, state, glow behavior, and Android layout.

**Out of scope:**

- New audio analysis, visualization modes, playback controls, or settings.
- Album art as a full screen mode.
- Landscape controls or adaptive changes to the existing NOW layout.
- Release assets and store listing changes.

## Design

### State

The NOW feature holds one selected `VisualizerDisplayMode` above the content that is replaced during expansion. The small display and expanded view read and update this same mode. Opening from `ART` changes it to `RADAR`; opening from another mode keeps that mode. While expanded, the mode cycle is `RADAR → BANDS → GRID → RADAR`, in both directions according to swipe direction. Closing leaves the current mode selected in NOW.

The open/closed flag and Exit visibility are temporary UI state, not a persisted setting. Exit starts hidden, appears when the user taps the expanded view, and hides about three seconds after the latest tap. The expanded view continues to consume audio analysis frames while visible. Ownership of the visualizer-active signal remains above the two views so replacing the NOW panel does not briefly stop analysis.

### Interface and Layout

The expanded view replaces `TerminalScaffold` while open and fills the window with the theme background. It renders the existing `TerminalVisualizerScene` using the same live `AudioAnalysisFrame` and effective effects setting. Radar and Grid use a centered square sized to the shorter available dimension; Spectrum fills the available portrait canvas. The square content retains its visual style without distortion.

A long press on the square NOW display opens the expanded view. Its existing short tap continues to cycle modes on NOW. A deliberate horizontal swipe inside the expanded view selects the adjacent audio mode; short or predominantly vertical movement does not change modes. A tap reveals Exit, and tapping Exit closes the view without triggering a visualization gesture. Android Back also closes it. Exit respects display cutouts and system gesture areas, and opening and exiting have accessible actions.

The app hides status and navigation bars during expansion and restores their prior visibility when the view closes or leaves composition. Android may temporarily reveal system bars through an edge gesture; the view remains usable when that happens. This follows Android's [immersive mode guidance](https://developer.android.com/develop/ui/compose/system/setup-e2e).

### Ambient Glow

Radar and Grid place a soft theme-colored glow behind the centered square in the unused upper and lower areas. Its brightness follows the existing `AudioAnalysisFrame.transient` value, which represents a short audio onset pulse, and falls back as that value decays. The value is clamped before use. The glow is decorative and does not change the visualizer geometry or add a new beat detector.

The glow follows the existing effective CRT and reduced motion behavior. It is absent when effects are disabled or audio analysis is idle or unavailable. Spectrum has no separate margin glow because it uses the full canvas.

### Error Handling and Edge Cases

- Idle playback shows the existing idle visualizer treatment. Unavailable analysis shows `SIGNAL UNAVAILABLE` with no ambient glow.
- Entry from album art begins at Radar, including when no track is playing.
- System Back and Exit both restore the normal NOW screen and system bars. Lifecycle disposal also restores the bars.
- Entry and exit preserve a continuous visualizer-active state while an audio mode remains visible.
- Swipe recognition avoids interpreting a tap, small movement, or primarily vertical gesture as a mode change. Android's edge navigation gestures remain available.

## Testing Strategy

- Add interaction tests for long press entry from art and each audio mode, forward and reverse swipe order, mode retention on return, tap to reveal Exit, timed hiding, and Exit and Back behavior.
- Test the visualizer-active handoff so analysis stays enabled across the NOW and expanded views.
- Test glow mapping, clamping, and its disabled, idle, unavailable, and reduced motion cases.
- Verify centered square geometry, full-height Spectrum, Exit placement near cutouts, and immersive bar restoration on a device or emulator using gesture navigation.
- Run the relevant automated tests and full test suite. All new code receives test coverage, and unrelated files remain untouched.

## Open Questions

None.
