# Editable Playback Queue Design

**Date:** 2026-08-18
**Status:** Approved

## Goal

Let users shape upcoming playback without interrupting the current track. Queue edits are expressed through a testable queue policy, applied to Media3, and included in the existing playback-state persistence.

## Success Criteria

- [ ] Tracks, albums, and playlists offer `PLAY NEXT` and `ADD TO QUEUE`.
- [ ] Collections are inserted as ordered blocks and duplicate tracks are preserved.
- [ ] Now Playing opens a dedicated queue editor.
- [ ] Users can jump to, remove, or drag-reorder upcoming tracks.
- [ ] Dragging provides accessible Move Up and Move Down actions.
- [ ] Any manual edit disables shuffle and makes the displayed order authoritative.
- [ ] Removing one item offers Undo; clearing upcoming items requires confirmation.
- [ ] The current and previously played tracks cannot be edited.
- [ ] Queue changes survive app and service recreation through existing playback persistence.
- [ ] Normal playback, repeat, seeking, notification controls, and visualizers remain unaffected.

## Scope

**In scope:**

- `PLAY NEXT` and `ADD TO QUEUE` actions for individual tracks, complete albums, and playlists.
- Ordered block insertion: `PLAY NEXT` inserts immediately after the current track; `ADD TO QUEUE` appends after all upcoming tracks.
- A dedicated queue-editor sub-screen launched from Now Playing.
- Jumping to an upcoming track.
- Removing one upcoming track with Undo.
- Clearing all upcoming tracks after confirmation.
- Drag reordering with accessible Move Up and Move Down alternatives.
- Duplicate queue entries.
- Automatic shuffle deactivation before any manual queue mutation.
- Persistence through the existing playback snapshot mechanism.
- Terminal-styled success, empty, confirmation, and error feedback.
- When nothing is loaded, either queue action establishes the supplied tracks as a paused queue without autoplaying.

**Out of scope:**

- Editing or deleting the current track and playback history.
- Saving the current queue as a playlist.
- Multi-select or bulk removal other than Clear Upcoming.
- Automatic deduplication.
- Queue synchronization between devices.
- Android Auto support.
- Favorites, listening history, richer search, personalization, and lyrics; each remains a separate feature design.

## Design

### Architecture

Media3 remains the runtime source of truth, while the existing playback snapshot remains the durable representation. A pure queue-editing policy calculates the intended upcoming order without depending on Android or Media3. The playback layer applies the policy result to Media3 and the existing persistence mechanism saves the resulting queue.

No Room entities or schema migration are required.

### Data Model and State

- **Queue entry:** An occurrence identifier plus track path, title, artist, album, and duration. Occurrence identifiers distinguish duplicate copies of the same track and remain stable during the active playback session.
- **Queue snapshot:** Ordered entries, current-entry position, shuffle state, and repeat state.
- **Queue command:** Insert Next, Append, Move, Remove, Clear Upcoming, or Restore Removed.
- **Queue result:** The new ordered entries, resulting current position, shuffle state, repeat state, and an optional user-facing notice.
- **Undo token:** The removed entry and its former upcoming position. Only the most recent single-item removal is undoable.
- **Queue-editor UI state:** Read-only current-track summary, ordered upcoming entries, Clear Upcoming availability, pending confirmation, optional removal Undo, and success or error feedback.

Occurrence identifiers are regenerated when a persisted queue is restored; persistence only requires track paths and order. Favorites, play counts, and listening history do not enter this model.

### Key Interfaces and Interactions

The pure queue-editing policy accepts a queue snapshot and one queue command. It validates that only upcoming entries are affected, applies the requested mutation, updates shuffle or repeat state where required, and returns the resulting state.

The playback boundary exposes these operations to UI surfaces:

- Play next with one or more tracks.
- Add one or more tracks to the end.
- Jump to an upcoming occurrence.
- Move an upcoming occurrence.
- Remove an upcoming occurrence.
- Restore the last removed occurrence.
- Clear all upcoming occurrences.

The playback layer projects Media3 state into queue-editor state and applies each validated policy result as one logical update. Existing playback snapshot persistence records the resulting order.

UI integration follows these rules:

- Track rows gain a compact queue-actions affordance.
- Album detail exposes `PLAY NEXT` and `ADD TO QUEUE` for playable tracks in disc and track order.
- Playlist detail exposes the same actions using playlist order while skipping unavailable entries with feedback.
- Now Playing replaces its static Up Next list with a summary and `[ QUEUE ]` command.
- The queue editor shows the current track separately, followed by draggable upcoming rows with Jump and Remove actions.
- Reordering uses a visible `[::]` drag handle plus accessible Move Up and Move Down actions.
- Clear Upcoming opens an explicit confirmation.
- Removing a row shows a temporary Undo notice.
- Manual queue mutation while shuffled reports that shuffle was disabled.

External MediaSession controls continue to operate normally and do not expose editor-specific commands.

### Queue Semantics

- Each `PLAY NEXT` request inserts immediately after the current track and ahead of earlier Play Next blocks. Internal collection order is preserved.
- `ADD TO QUEUE` appends after every existing upcoming entry.
- Duplicate tracks remain separate occurrences and can be moved or removed independently.
- The current track and previously played tracks remain fixed.
- Every manual mutation turns shuffle off before applying the displayed order.
- Clear Upcoming also turns Repeat All off so playback ends after the current track. Repeat One remains unchanged.
- With no loaded queue, Play Next or Add to Queue establishes an ordered, paused queue without autoplaying.

### Error Handling and Edge Cases

- Empty selections and collections with no playable tracks leave the queue unchanged and show an actionable notice.
- Unavailable album or playlist entries are skipped; feedback reports how many tracks were queued and skipped.
- If playback advances while a row is being dragged, the drag is cancelled and the editor refreshes rather than risking an edit to the wrong occurrence.
- If an entry becomes unavailable before an operation is applied, the editor refreshes and reports that the queue changed.
- Undo reinserts the removed occurrence near its former upcoming position. If playback has advanced, that position is clamped to the remaining upcoming range.
- Only the most recent single removal can be undone. Starting another removal or leaving the editor expires the prior Undo action.
- A failed Media3 mutation leaves the active queue unchanged, reloads authoritative player state, and shows an error.
- Snapshot persistence failure does not interrupt playback. The runtime queue remains valid, with restoration failure handled on the next service start.

## Testing Strategy

All new production behavior is introduced through failing behavior-focused tests.

### Pure Policy Unit Tests

- Insert Next and Append preserve collection order.
- Repeated Play Next requests place the newest block first.
- Duplicate paths receive independent occurrence identities.
- Move, remove, restore, and clear affect upcoming entries only.
- Attempts to modify current or historical entries are rejected.
- Every manual edit disables shuffle.
- Clear Upcoming disables Repeat All.
- Undo restores the correct position and clamps safely after playback advances.
- Empty or invalid inputs produce no queue mutation.

### Playback Integration Tests

- Policy results are applied to Media3 in the intended order.
- Jump selects the requested occurrence, including duplicate tracks.
- Queue edits update observable editor state.
- Edited queues, duplicates, position, shuffle, and repeat state survive service recreation.
- Failed or stale mutations reload authoritative player state without interrupting playback.
- Existing notification, headset, seek, repeat, and visualizer behavior remains unchanged.

### UI Tests

- Track, album, and playlist surfaces expose both queue actions.
- Now Playing opens and returns from the dedicated queue editor.
- Drag and accessible Move Up and Move Down produce the same ordering.
- Remove presents Undo; Clear presents confirmation.
- Empty, partial-success, shuffle-disabled, and error notices are readable.
- Current and historical tracks cannot be edited.

### Device Verification

- Exercise long queues and drag behavior on the Pixel 7.
- Edit while playback transitions between tracks.
- Lock and reopen the device, kill and recreate the app, and confirm queue restoration.
- Confirm notification controls remain responsive throughout editing.

## Open Questions

None.
