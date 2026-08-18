# Queue Drag Reordering Design

**Date:** 2026-08-18
**Status:** Approved

## Goal

Make queue reordering direct and predictable on touch devices. Dragging an upcoming track from its existing `[::]` handle lifts the row under the user's finger, previews its destination by shifting surrounding rows, supports edge auto-scroll, and commits one queue move when released without changing playback or queue persistence semantics.

## Success Criteria

- [ ] A track can be dragged continuously across any number of upcoming queue rows.
- [ ] The dragged row visually lifts and follows the user's finger.
- [ ] Surrounding rows shift to preview the proposed queue order.
- [ ] Holding the dragged row near a list edge automatically scrolls a long queue.
- [ ] Releasing the row commits exactly one move to the previewed destination.
- [ ] Cancelled or stale gestures do not mutate the queue.
- [ ] Existing accessible Move Up and Move Down actions remain available.

## Scope

**In scope:**

- Upcoming tracks on the Queue Editor screen.
- Continuous handle-based dragging across multiple rows.
- A lifted visual state using translation, emphasis, and elevation consistent with the terminal theme.
- Animated movement of surrounding rows.
- Edge-triggered automatic scrolling.
- One queue mutation when a track is dropped.
- Gesture cancellation when the queue or current-track identity changes incompatibly.
- Existing Move Up/Down accessibility actions and Jump/Remove controls.

**Out of scope:**

- Reordering the currently playing track.
- Dragging from anywhere besides the handle.
- Changing queue persistence or playback semantics.
- Reordering playlists or other lists.
- Adding a third-party reorder library.
- New vibration or sound feedback.

## Design

### Interaction

Dragging starts only from the existing `[::]` handle. The selected queue row lifts above the list and follows the finger continuously. As it crosses other rows, those rows shift to preview the new order. The drag may span any number of rows and automatically scrolls the queue when held near its top or bottom edge.

The proposed order remains temporary during the gesture. Releasing the row commits one move to the previewed destination. Cancelling the gesture restores the authoritative order without a queue mutation.

### State

The playback queue remains the authoritative, persisted order. The screen owns temporary drag state containing:

- The dragged track's occurrence ID, keeping duplicate tracks distinct.
- Its starting and current proposed indexes.
- The finger's vertical offset.
- Measured row positions and list bounds.
- Whether edge auto-scroll is active.
- The current-track occurrence ID captured when dragging begins.
- The occurrence order captured when dragging begins.

Crossing a row's midpoint updates only the temporary displayed order. If the queue refreshes unexpectedly, the occurrence order becomes incompatible, or the current-track occurrence changes, the temporary state is discarded and the latest playback order is shown.

### Interfaces

`QueueEditorScreen` retains its existing queue callbacks. Reordering continues to produce the existing `onMove(occurrenceId, targetIndex, expectedCurrentOccurrenceId)` call, but invokes it only once when a valid drag ends.

Internally, the queue list owns a temporary drag coordinator. Each row reports its visible position and supplies the handle with drag start, movement, cancellation, and release events. The coordinator determines the proposed index, visual offsets, lifted-row styling, and edge-scroll direction.

The handle consumes the reorder gesture while Jump and Remove remain independently tappable. Accessibility Move Up and Move Down actions continue to call `onMove` immediately for one position.

### Visual Behavior

The real queue row is translated with the finger rather than rendering a detached duplicate. While active, it receives emphasis and elevation appropriate to the existing terminal UI and draws above neighboring rows. Other rows animate into the space indicated by the temporary order. On a valid release, the lifted row settles into its destination; on cancellation, it returns to the authoritative position.

### Error Handling and Edge Cases

- Releasing without crossing a row boundary makes no queue call.
- Dragging beyond either end clamps the destination to the first or last upcoming position.
- Edge scrolling stops when the finger leaves the edge region, the list reaches its boundary, or the gesture ends.
- Gesture cancellation restores the original displayed order.
- If playback advances, the current track changes, or the dragged occurrence disappears, the drag is cancelled and refreshed queue state wins.
- Duplicate tracks are identified by occurrence ID, never by path or title.
- A queue update during the gesture cancels the preview when its occurrence order no longer matches the drag's starting order.
- Jump, Remove, Back, and Clear cannot accidentally fire from the drag gesture.
- A rejected or failed move uses the existing playback refresh and notice behavior.
- With one upcoming track, the handle remains accessible but dragging causes no mutation.

## Testing Strategy

Automated Compose tests will verify:

- Dragging across multiple rows produces one final move callback.
- Releasing within the original position produces no callback.
- Upward and downward drags resolve to the correct destination.
- Destinations clamp at the first and last upcoming positions.
- Gesture cancellation and queue/current-track changes produce no stale move.
- Duplicate tracks reorder by occurrence ID.
- Accessibility Move Up/Down actions still move exactly one position.
- Jump and Remove remain independent from dragging.
- Lifted-row and displaced-row state is exposed through stable test semantics where pixel-perfect gestures would be brittle.

A screenshot test will cover the lifted terminal-style row appearance. Manual device verification will cover finger tracking, smooth displacement, and edge auto-scroll on a long queue because those physical interactions are not fully represented by Compose test input.

## Open Questions

None.
