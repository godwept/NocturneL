# Playlist Detail Drag and Compact Rows Design

**Date:** 2026-08-19
**Status:** Approved

## Goal

Make Playlist Detail easier to scan and reorder by giving it the same direct drag interaction as Queue Editor and reducing each existing-playlist entry to a compact, single-line row containing only a drag handle, track identity, and remove control.

## Success Criteria

- [ ] Each playlist entry displays `[::] Artist :: Track title [X]` on one row.
- [ ] Dragging from `[::]` provides the same lift, live displacement, edge auto-scroll, cancellation, and drop behavior as Queue Editor.
- [ ] A completed drag invokes exactly one existing playlist move operation.
- [ ] Tapping `[X]` removes the correct entry.
- [ ] FAV, `+Q`, play count, duration, and visible up/down buttons are absent from playlist entries.
- [ ] Accessible Move Up and Move Down actions remain available on the drag handle.
- [ ] Missing or unavailable entries remain identifiable, reorderable, and removable.
- [ ] Add Track results retain their existing layout and behavior.

## Scope

**In scope:**

- Existing playlist-entry rows on Playlist Detail.
- A shared internal drag-reorder foundation used by Queue Editor and Playlist Detail.
- Single-line `Artist :: Track title` text with ellipsis when space is limited.
- `[::]` drag handle, `[X]` remove control, lifted styling, animated displacement, and edge auto-scroll.
- Removal of playlist-only favorite, per-track queue, play-count, duration, and visible arrow-control wiring.
- Tests for drag behavior, accessibility actions, compact layout, removal, unavailable entries, and Queue Editor regressions.

**Out of scope:**

- Changes to the Add Track picker.
- Changes to playlist-level Play, Rename, Add Track, or Add Queue actions.
- Changes to playlist persistence or repository move semantics.
- Dragging from anywhere except `[::]`.
- Changes to Album Detail, Search, or Queue Editor's visible row content.
- New vibration, sound, or third-party reorder dependencies.

## Design

### Interaction and presentation

Each existing playlist entry is rendered as one compact row:

```text
[::] Artist :: Track title [X]
```

The text occupies the flexible middle region, stays on one line, and ellipsizes before the fixed remove control. The drag begins only on `[::]`. It matches Queue Editor completely: the selected row lifts and follows the finger, neighboring rows shift to preview the destination, holding near an edge scrolls a long list, and release commits one move. The proposed order remains temporary until release.

The drag handle exposes accessible Move Up and Move Down actions where applicable. The visible arrow controls, per-track FAV and `+Q` controls, play count, and duration are removed. Add Track results keep their current `[+] Artist :: Track title` layout and behavior.

### State

The shared drag foundation operates on an ordered list of unique row keys and owns temporary UI state containing:

- The dragged row key.
- Starting and preview orders.
- Starting and target indexes.
- Current vertical translation.
- Visible row bounds and list viewport information.
- Edge-scroll velocity.

Queue Editor continues using occurrence IDs and retains its current-track and authoritative-queue compatibility checks.

Playlist Detail creates snapshot-local keys from each entry's starting position and path. This keeps duplicate paths distinct during a gesture without adding persistent IDs or changing the database model. On drop, the screen converts the dragged entry back to its original playlist position and invokes the existing `onMove(fromPosition, targetIndex)` callback exactly once.

The playlist repository remains authoritative. If the authoritative entry order changes during a drag, the gesture is cancelled and the refreshed state wins. No mutation occurs while the finger is moving.

### Interfaces

`PlaylistDetailScreen` retains its playlist-level actions and entry callbacks, including:

- `onMove(fromPosition, targetIndex)`
- `onRemove(position)`
- Play, rename, add-track, and add-playlist-to-queue callbacks

It drops the per-entry queue and favorite inputs:

- `onAddTrackToQueue`
- `favoriteTrackPaths`
- `trackPlayCounts`
- `onToggleTrackFavorite`

These unused parameters are also removed from `PlaylistsScreen` and its app-level call site.

The queue-specific drag code is separated into shared internal drag primitives and thin screen-specific adapters. The shared layer owns gesture calculations, preview ordering, translation, edge scrolling, and visual drag state. Queue Editor retains its playback-specific validation and callback signature; Playlist Detail retains its position-based repository callback.

`PlaylistTrackRow` retains only the data required by the resulting screen and its operations. Duration and per-track action data are not carried solely for removed presentation.

### Error handling and edge cases

- Dropping a row without changing its index performs no move.
- Destinations clamp to the first and last playlist positions.
- Cancelling a gesture restores the authoritative playlist order.
- A playlist refresh or incompatible entry-order change during dragging cancels the active gesture.
- Duplicate paths are treated as separate entries using snapshot-local keys.
- Missing or unavailable entries use their existing fallback artist and title values and remain reorderable and removable.
- A one-entry playlist retains the accessible handle, but dragging produces no mutation.
- Edge auto-scroll stops when the gesture ends, leaves the edge region, or reaches the list boundary.
- Remove cannot fire accidentally during a handle drag, and removing an entry during an active drag invalidates that gesture.
- Long `Artist :: Track title` text stays on one line and ellipsizes before the fixed remove button.
- Repository failures continue through existing refresh and message behavior; no new persistence or retry mechanism is introduced.

## Testing Strategy

Implementation will follow TDD, beginning with failing tests for the shared reorder behavior and compact playlist UI.

Automated coverage will verify:

- Shared drag calculations preserve Queue Editor behavior.
- Playlist drags upward and downward commit exactly one correct `onMove` call.
- No-op drops and cancelled or stale gestures do not invoke `onMove`.
- Duplicate and unavailable entries remain independently reorderable and removable.
- First and last boundaries clamp correctly.
- Accessible Move Up and Move Down actions remain on the `[::]` handle.
- FAV, `+Q`, play count, duration, and visible arrow buttons are absent.
- Existing rows render one ellipsized `Artist :: Track title` line.
- Add Track results and playlist-level actions remain unchanged.
- Queue Editor regression tests continue to pass after extraction.
- The Playlist Detail screenshot reference is updated and inspected for density, alignment, truncation, and drag styling.

Final verification will include the relevant unit tests, Compose tests, screenshot validation, lint, and debug build.

## Open Questions

None.
