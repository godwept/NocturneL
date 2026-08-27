# Visualizer Sync Hold-to-Repeat Design

**Date:** 2026-08-26
**Status:** Approved

## Goal

Let users traverse large visualizer synchronization offsets quickly by holding the existing minus or plus button, while preserving precise 25 ms taps and the current unobtrusive overlay.

## Success Criteria

- [ ] Touch-down immediately changes the offset by 25 ms.
- [ ] Continued holding begins auto-repeat after approximately 400 ms.
- [ ] Repetition runs every 100 ms initially and accelerates to every 50 ms after approximately 1.5 seconds.
- [ ] Releasing or cancelling the gesture stops adjustment immediately.
- [ ] Holding from `0 ms` to approximately `+1400 ms` takes only a few seconds.
- [ ] The offset never exceeds the existing `-2000..+2000 ms` range.
- [ ] A tap performs exactly one adjustment.
- [ ] Sync-control gestures never cycle the visualization mode.
- [ ] Existing accessibility click actions remain available.
- [ ] No new controls or permanent UI elements are introduced.

## Scope

**In scope:**

- Add accelerating press-and-hold behavior to the existing visualizer minus and plus corner buttons.
- Preserve the existing 25 ms callbacks, live visualizer updates, persistence, transient value label, button placement, and limit states.
- Stop repeating on release, pointer cancellation, movement outside the active control, disposal, or reaching the applicable limit.
- Keep the hold behavior compatible with the controls' existing accessibility actions.
- Add automated interaction coverage and update the visualizer manual.

**Out of scope:**

- Offset presets.
- Per-device or per-Bluetooth-route offsets.
- Automatic latency detection.
- New buttons, menus, dialogs, or settings.
- Changes to the offset range, step size, playback timing, or visualizer rendering.
- Haptic or audio feedback.

## Design

### State and Behavior

The persisted visualizer offset remains the only application-level state. Press-and-hold adds temporary gesture state local to each corner control: whether it is pressed, when the press began, and whether its repeat loop is active.

On touch-down, the control invokes its existing adjustment callback once. If the press remains active, it waits 400 ms and then invokes the same callback every 100 ms. At 1.5 seconds from touch-down, the interval becomes 50 ms.

Every adjustment continues through the existing ViewModel and preference path, keeping the displayed value, persisted value, and active visualizer synchronized. Each real adjustment refreshes the transient `VIS SYNC` label. After release, the label fades three seconds after the final change.

The repeat loop is bound to the button's gesture and Compose lifecycle. It is cancelled immediately when the finger lifts, the gesture is cancelled or leaves the control, the button becomes disabled at its limit, or the composable leaves the screen. Only one direction can own an active gesture at a time.

### Interfaces

`VisualizerDeck` retains its existing application-level callbacks:

- `onDecreaseSyncOffset: () -> Unit`
- `onIncreaseSyncOffset: () -> Unit`
- `onResetSyncOffset: () -> Unit`

The interaction change remains inside the visualizer UI. The existing private corner-button control gains pointer press-and-hold handling governed by these timing values:

- Initial repeat delay: 400 ms.
- Normal repeat interval: 100 ms.
- Accelerated repeat interval: 50 ms.
- Acceleration threshold: 1500 ms from touch-down.

The control continues to expose a normal semantic click action for keyboard, switch-access, and screen-reader users. A semantic click performs one 25 ms adjustment; touch holding supplements rather than replaces accessible click behavior.

No repository, ViewModel, playback, database, or public composable interface changes are required.

### Error Handling and Edge Cases

- At `-2000 ms` or `+2000 ms`, the corresponding button remains disabled and cannot start a repeat gesture.
- If a hold reaches a limit, repeating stops rather than continuing to invoke no-op callbacks.
- A quick touch adjusts exactly once; lifting does not trigger a second adjustment.
- Moving outside the pressed button cancels the hold and causes no release adjustment.
- Pointer cancellation, mode changes, navigation, and composable disposal stop the repeat loop immediately.
- Rapidly pressing opposite buttons cannot leave an earlier repeat loop running.
- Recompositions caused by offset updates do not restart the gesture timer or produce duplicate adjustments.
- The transient label refreshes for real adjustments but does not keep restarting after a limit is reached.
- Touches consumed by either sync button never reach the visualization mode-cycling layer.
- Non-touch semantic clicks retain their existing one-step behavior.

## Testing Strategy

Focused Compose interaction tests verify that a quick press changes the offset once, touch-down changes it immediately, no repeat occurs before 400 ms, repetition follows the 100 ms interval, and a hold beyond 1.5 seconds accelerates to the 50 ms interval. Further tests verify cancellation on release, pointer cancellation, and movement outside; stopping and disabling at both limits; mode-cycle isolation; transient-label timing; and one-step semantic accessibility clicks.

Existing offset-policy, persistence, visualizer-layout, screenshot, and playback-alignment tests provide regression coverage. The visualizer manual documents tap for 25 ms precision and hold for accelerated adjustment. A device check confirms that reaching approximately `+1400 ms` feels quick and controllable without accidental overshoot.

## Open Questions

Offset presets remain a deferred wishlist item and require a separate design before implementation.
