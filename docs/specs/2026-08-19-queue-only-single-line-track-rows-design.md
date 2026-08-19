# Queue-Only Single-Line Track Rows Design

**Date:** 2026-08-19
**Status:** Approved

## Goal

Simplify track-list controls by making append-to-queue the only queue insertion action and keep track information compact and scannable by preventing text wrapping. Users can append tracks, albums, or playlists and then use Queue Editor to place entries where they want them.

## Success Criteria

- [ ] Per-track `[ NXT ]` is absent from Album Detail, Search, and Playlist Detail.
- [ ] Album- and playlist-level `[ PLAY NEXT ]` actions are absent.
- [ ] `[ +Q ]` remains the sole per-track queue action.
- [ ] Album Detail shows `[ BACK ]`, `[ PLAY ]`, `[ SHUFFLE ]`, and `[ ADD QUEUE ]` on one action line.
- [ ] Playlist Detail shows `[ BACK ]` on a standalone top row and `[ PLAY ]`, `[ RENAME ]`, `[ ADD TRACK ]`, and `[ ADD QUEUE ]` on one action line.
- [ ] Track titles and combined track-result text never wrap; overflow ends with an ellipsis.
- [ ] Existing play, shuffle, append-to-queue, favorite, reorder, remove, jump, undo, and unavailable-track behavior remains intact.

## Scope

**In scope:**

- Remove every user-facing `NXT` and `PLAY NEXT` action from Album Detail, Search, and Playlist Detail.
- Remove the corresponding UI callbacks, app wiring, playback insertion mode, queue policy command, and tests that become unnecessary.
- Keep Queue Editor and its manual move/reorder behavior.
- Reorganize Album Detail and Playlist Detail controls as approved.
- Apply single-line ellipsis to track-title or combined track-description text in Album Detail, Search results, Playlist Detail entries and available-track results, favorites, recent listening, listening history, and Queue Editor.
- Preserve intentionally separate metadata lines while preventing each individual line from wrapping.

**Out of scope:**

- Changing append order or Queue Editor reorder mechanics.
- Changing album or artist headings.
- Changing Now Playing's existing marquee.
- Adding popups, long-press behavior, or another visual expansion mechanism for truncated titles.
- Redesigning non-track lists.

## Design

### State and Behavior Boundaries

No new state is required. Queueing becomes a single append-only user path:

- Per-track `[ +Q ]` appends one playable track.
- Album- and playlist-level `[ ADD QUEUE ]` append all playable tracks in their existing order.
- Users may then move entries in Queue Editor.

Because no UI retains a play-next action, remove its unused callback parameters and wiring, `PlaybackConnection.playNext`, the private `NEXT` enqueue mode, the `InsertNext` queue command, and their dedicated tests. Preserve append, reorder, undo, jump, and remove commands and behavior.

Ellipsis is presentation-only. Complete titles remain in entities and view state and continue to be used by click handlers, content descriptions, and Compose semantics.

### Interfaces and Layout

`QueueTrackActions` becomes an append-only component accepting the track title, append callback, modifier, and its existing optional play-count and favorite inputs. Its only queue button is `[ +Q ]`, with the full `Add {title} to queue` accessibility description.

Album Detail uses one action line:

```text
[ BACK ] [ PLAY ] [ SHUFFLE ] [ ADD QUEUE ]
```

Playlist Detail uses two control lines:

```text
[ BACK ]
[ PLAY ] [ RENAME ] [ ADD TRACK ] [ ADD QUEUE ]
```

Screen contracts drop `onPlayNext`, `onPlayAlbumNext`, and `onPlayTrackNext` parameters. Remaining queue callbacks keep their existing append-oriented signatures.

Each bounded track-text region uses `maxLines = 1` and `TextOverflow.Ellipsis`. Weighted text receives the remaining width before fixed controls, ensuring text truncates instead of displacing buttons. Playlist title and artist/duration remain two intentional lines, with each line independently constrained.

### Error Handling and Edge Cases

- Empty albums and playlists keep `ADD QUEUE` disabled.
- Missing or otherwise unplayable tracks do not show `[ +Q ]` and are excluded from album or playlist queue operations.
- Long titles may reduce to only a few visible characters plus an ellipsis when fixed controls consume most of the width; controls retain their minimum touch targets.
- Track numbers, durations, counts, favorite controls, reorder controls, and queue controls remain visible; the title region yields space first.
- Search's combined `artist :: title · play count` result remains one line and ellipsizes at the end.
- Queue Editor, listening history, favorites, and recent-track rows use the same single-line rule for each title or metadata line.
- Truncation does not change stored titles, sorting, searching, playback metadata, queue data, or accessibility wording.
- Removing insert-next behavior does not alter append order, undo snapshots, or manual reordering.

## Testing Strategy

- Update shared queue-action tests to prove `[ NXT ]` and its accessibility action are absent while `[ +Q ]` still invokes append.
- Update Album Detail tests to verify one action row contains `BACK`, `PLAY`, `SHUFFLE`, and `ADD QUEUE`, with no `PLAY NEXT`.
- Update Playlist Detail tests to verify standalone `BACK`, a single action row containing `PLAY`, `RENAME`, `ADD TRACK`, and `ADD QUEUE`, and no `PLAY NEXT`.
- Update Search and app-wiring tests to remove play-next callback expectations.
- Add long-title Compose cases for Album Detail, Search, Playlist Detail, listening rows, and Queue Editor. Verify one rendered line, bounded width before fixed controls, and complete semantic text.
- Update queue-policy unit tests after removing `InsertNext`, retaining append, reorder, remove, jump, and undo coverage.
- Update affected deterministic 412dp screenshot references and inspect them for one-line titles, ellipsis, aligned actions, preserved touch targets, and clipping.
- Run unit tests, Android-test compilation and connected tests, screenshot validation, lint, and the debug build.

## Open Questions

None.
